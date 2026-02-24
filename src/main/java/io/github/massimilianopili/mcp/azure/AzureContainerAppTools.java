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
public class AzureContainerAppTools {

    private static final String API_VERSION = "2024-03-01";
    private static final String PROVIDER    = "/providers/Microsoft.App/containerApps";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureContainerAppTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_container_apps",
          description = "Elenca tutte le Container App nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listContainerApps() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> apps = (List<Map<String, Object>>) response.get("value");
                    return apps.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", a.getOrDefault("name", ""));
                        r.put("location", a.getOrDefault("location", ""));
                        r.put("provisioningState", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("provisioningState", "")
                                : "");
                        r.put("fqdn", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("latestRevisionFqdn", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Container App: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_container_app",
          description = "Recupera i dettagli di una Container App Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getContainerApp(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Container App") String appName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Container App: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_container_app_revisions",
          description = "Elenca le revisioni di una Container App Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listContainerAppRevisions(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Container App") String appName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName
                        + "/revisions?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> revisions = (List<Map<String, Object>>) response.get("value");
                    return revisions.stream().map(rev -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", rev.getOrDefault("name", ""));
                        r.put("active", rev.containsKey("properties")
                                ? ((Map<String, Object>) rev.get("properties")).getOrDefault("active", false)
                                : false);
                        r.put("createdTime", rev.containsKey("properties")
                                ? ((Map<String, Object>) rev.get("properties")).getOrDefault("createdTime", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista revisioni Container App: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_restart_container_app",
          description = "Riavvia una revisione di una Container App Azure")
    public Mono<Map<String, Object>> restartContainerApp(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Container App") String appName,
            @ToolParam(description = "Nome della revisione da riavviare") String revisionName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName
                        + "/revisions/" + revisionName + "/restart?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "message", "Riavvio revisione " + revisionName + " avviato"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore riavvio Container App: " + e.getMessage())));
    }
}
