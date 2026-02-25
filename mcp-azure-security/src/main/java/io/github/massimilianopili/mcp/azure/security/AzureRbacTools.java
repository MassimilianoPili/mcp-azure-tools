package io.github.massimilianopili.mcp.azure.security;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureRbacTools {

    private static final String API_VERSION = "2022-04-01";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureRbacTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_role_assignments",
          description = "Elenca le assegnazioni di ruolo (RBAC) nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listRoleAssignments() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Authorization/roleAssignments?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> assignments = (List<Map<String, Object>>) response.get("value");
                    return assignments.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", a.getOrDefault("name", ""));
                        r.put("principalId", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("principalId", "")
                                : "");
                        r.put("roleDefinitionId", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("roleDefinitionId", "")
                                : "");
                        r.put("scope", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("scope", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista role assignment: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_create_role_assignment",
          description = "Crea una nuova assegnazione di ruolo RBAC in Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createRoleAssignment(
            @ToolParam(description = "GUID univoco per l'assegnazione (genera un UUID v4)") String assignmentGuid,
            @ToolParam(description = "ID completo della role definition, es: /subscriptions/{id}/providers/Microsoft.Authorization/roleDefinitions/{roleId}") String roleDefinitionId,
            @ToolParam(description = "Object ID del principal (utente, gruppo, service principal) a cui assegnare il ruolo") String principalId) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("roleDefinitionId", roleDefinitionId);
        properties.put("principalId", principalId);

        return webClient.put()
                .uri(props.getArmBase() + "/providers/Microsoft.Authorization/roleAssignments/" + assignmentGuid + "?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("properties", properties))
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione role assignment: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_role_assignment",
          description = "Elimina una assegnazione di ruolo RBAC da Azure")
    public Mono<Map<String, Object>> deleteRoleAssignment(
            @ToolParam(description = "Nome (GUID) dell'assegnazione di ruolo da eliminare") String assignmentName) {
        return webClient.delete()
                .uri(props.getArmBase() + "/providers/Microsoft.Authorization/roleAssignments/" + assignmentName + "?api-version=" + API_VERSION)
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "deleted", true))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione role assignment: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_role_definitions",
          description = "Elenca le definizioni di ruolo RBAC disponibili nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listRoleDefinitions() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Authorization/roleDefinitions?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> roles = (List<Map<String, Object>>) response.get("value");
                    return roles.stream().map(role -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", role.getOrDefault("name", ""));
                        r.put("displayName", role.containsKey("properties")
                                ? ((Map<String, Object>) role.get("properties")).getOrDefault("roleName", "")
                                : "");
                        r.put("type", role.containsKey("properties")
                                ? ((Map<String, Object>) role.get("properties")).getOrDefault("type", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista role definition: " + e.getMessage()))));
    }
}
