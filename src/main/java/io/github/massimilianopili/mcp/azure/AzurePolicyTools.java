package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzurePolicyTools {

    private static final String API_VERSION = "2023-04-01";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzurePolicyTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_policy_assignments",
          description = "Elenca le assegnazioni di policy nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPolicyAssignments() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Authorization/policyAssignments?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> assignments = (List<Map<String, Object>>) response.get("value");
                    return assignments.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", a.getOrDefault("name", ""));
                        r.put("displayName", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("displayName", "")
                                : "");
                        r.put("scope", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("scope", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista policy assignment: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_policy_assignment",
          description = "Recupera i dettagli di una specifica assegnazione di policy Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getPolicyAssignment(
            @ToolParam(description = "Nome dell'assegnazione di policy") String assignmentName) {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Authorization/policyAssignments/" + assignmentName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero policy assignment: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_policy_definitions",
          description = "Elenca le definizioni di policy built-in disponibili in Azure (prime 50)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPolicyDefinitions() {
        return webClient.get()
                .uri("https://management.azure.com/providers/Microsoft.Authorization/policyDefinitions?$top=50&api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> definitions = (List<Map<String, Object>>) response.get("value");
                    return definitions.stream().map(d -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", d.getOrDefault("name", ""));
                        r.put("displayName", d.containsKey("properties")
                                ? ((Map<String, Object>) d.get("properties")).getOrDefault("displayName", "")
                                : "");
                        r.put("policyType", d.containsKey("properties")
                                ? ((Map<String, Object>) d.get("properties")).getOrDefault("policyType", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista definizioni policy: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_create_policy_assignment",
          description = "Assegna una policy definition a uno scope Azure (subscription o resource group)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createPolicyAssignment(
            @ToolParam(description = "Nome dell'assegnazione (identificativo univoco)") String assignmentName,
            @ToolParam(description = "ID completo della policy definition da assegnare") String policyDefinitionId,
            @ToolParam(description = "Scope: /subscriptions/{id} o /subscriptions/{id}/resourceGroups/{rg}") String scope,
            @ToolParam(description = "Nome display dell'assegnazione", required = false) String displayName) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("policyDefinitionId", policyDefinitionId);
        properties.put("scope", scope);
        if (displayName != null && !displayName.isBlank()) {
            properties.put("displayName", displayName);
        }

        return webClient.put()
                .uri(props.getArmBase() + "/providers/Microsoft.Authorization/policyAssignments/" + assignmentName + "?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("properties", properties))
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione policy assignment: " + e.getMessage())));
    }
}
