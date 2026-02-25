package io.github.massimilianopili.mcp.azure.core;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureDeploymentTools {

    private static final String API_VERSION = "2024-03-01";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureDeploymentTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_deployments",
          description = "Elenca le ARM deployment in un resource group Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listDeployments(
            @ToolParam(description = "Nome del resource group") String resourceGroup) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.Resources/deployments?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> deps = (List<Map<String, Object>>) response.get("value");
                    return deps.stream().map(d -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", d.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) d.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("mode", p.getOrDefault("mode", ""));
                        r.put("timestamp", p.getOrDefault("timestamp", ""));
                        r.put("duration", p.getOrDefault("duration", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista deployments: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_deployment",
          description = "Recupera i dettagli di una ARM deployment Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getDeployment(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del deployment") String deploymentName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.Resources/deployments/" + deploymentName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero deployment: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_deployment",
          description = "Esegue una ARM template deployment in un resource group Azure (mode: Incremental o Complete)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createDeployment(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del deployment") String deploymentName,
            @ToolParam(description = "Modalità: Incremental (aggiunge/aggiorna) o Complete (ricrea tutto)") String mode,
            @ToolParam(description = "ARM template JSON come stringa") String templateJson,
            @ToolParam(description = "Parametri del template JSON come stringa (es: {\"param1\":{\"value\":\"val\"}})") String parametersJson) {
        Map<String, Object> body;
        try {
            body = Map.of("properties", Map.of(
                    "mode", mode,
                    "template", templateJson,
                    "parameters", parametersJson
            ));
        } catch (Exception e) {
            return Mono.just(Map.of("error", "Parametri non validi: " + e.getMessage()));
        }
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.Resources/deployments/" + deploymentName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione deployment: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_deployment",
          description = "Elimina una ARM deployment dalla history di un resource group Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteDeployment(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del deployment da eliminare") String deploymentName) {
        return webClient.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.Resources/deployments/" + deploymentName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminato: " + deploymentName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione deployment: " + e.getMessage())));
    }
}
