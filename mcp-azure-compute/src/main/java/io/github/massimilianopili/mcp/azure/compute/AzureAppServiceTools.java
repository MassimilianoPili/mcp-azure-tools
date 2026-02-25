package io.github.massimilianopili.mcp.azure.compute;

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
public class AzureAppServiceTools {

    private static final String API_VERSION = "2023-12-01";
    private static final String PROVIDER = "/providers/Microsoft.Web/sites";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureAppServiceTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_app_services",
          description = "Elenca tutti gli App Service (web app) nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAppServices() {
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
                        r.put("kind", a.getOrDefault("kind", ""));
                        r.put("state", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("state", "")
                                : "");
                        r.put("defaultHostName", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("defaultHostName", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista App Service: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_app_service",
          description = "Recupera i dettagli di un App Service Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAppService(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'App Service") String appName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero App Service: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_start_app_service",
          description = "Avvia un App Service Azure")
    public Mono<Map<String, Object>> startAppService(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'App Service") String appName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName + "/start?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "message", "App Service " + appName + " avviato"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore avvio App Service: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_stop_app_service",
          description = "Arresta un App Service Azure")
    public Mono<Map<String, Object>> stopAppService(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'App Service") String appName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName + "/stop?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "message", "App Service " + appName + " arrestato"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore arresto App Service: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_restart_app_service",
          description = "Riavvia un App Service Azure")
    public Mono<Map<String, Object>> restartAppService(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'App Service") String appName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName + "/restart?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "message", "App Service " + appName + " riavviato"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore riavvio App Service: " + e.getMessage())));
    }
}
