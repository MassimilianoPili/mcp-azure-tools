package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Tool per Azure Diagnostic Settings.
 * Le diagnostic settings sono una "resource extension": si applicano a qualsiasi
 * risorsa ARM usando il pattern {resourceId}/providers/Microsoft.Insights/diagnosticSettings.
 * Permettono di inviare log e metriche di una risorsa a Log Analytics, Storage Account o Event Hub.
 */
@Service
public class AzureDiagnosticTools {

    private static final String API_VERSION = "2021-05-01-preview";
    private static final String DIAG_PROVIDER = "/providers/Microsoft.Insights/diagnosticSettings";

    private final WebClient webClient;

    public AzureDiagnosticTools(
            @Qualifier("azureArmWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @ReactiveTool(name = "azure_list_diagnostic_settings",
          description = "Elenca le diagnostic settings di una risorsa ARM (quali log/metriche vengono inviati e dove)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listDiagnosticSettings(
            @ToolParam(description = "Resource ID ARM completo della risorsa, es: /subscriptions/{sub}/resourceGroups/{rg}/providers/Microsoft.Sql/servers/{server}") String resourceId) {
        return webClient.get()
                .uri("https://management.azure.com" + resourceId + DIAG_PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> settings = (List<Map<String, Object>>) response.get("value");
                    return settings.stream().map(s -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", s.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) s.getOrDefault("properties", Map.of());
                        r.put("workspaceId", p.getOrDefault("workspaceId", ""));
                        r.put("storageAccountId", p.getOrDefault("storageAccountId", ""));
                        r.put("eventHubName", p.getOrDefault("eventHubName", ""));
                        r.put("logs", ((List<Map<String, Object>>) p.getOrDefault("logs", List.of()))
                                .stream().map(l -> l.getOrDefault("category", "")).toList());
                        r.put("metrics", ((List<Map<String, Object>>) p.getOrDefault("metrics", List.of()))
                                .stream().map(m -> m.getOrDefault("category", "")).toList());
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista diagnostic settings: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_diagnostic_settings",
          description = "Recupera i dettagli di una specifica diagnostic setting di una risorsa ARM")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getDiagnosticSettings(
            @ToolParam(description = "Resource ID ARM completo della risorsa") String resourceId,
            @ToolParam(description = "Nome della diagnostic setting") String settingName) {
        return webClient.get()
                .uri("https://management.azure.com" + resourceId + DIAG_PROVIDER + "/" + settingName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero diagnostic settings: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_diagnostic_settings",
          description = "Crea o aggiorna diagnostic settings per una risorsa ARM — invia tutti i log e metriche a un workspace Log Analytics")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createDiagnosticSettings(
            @ToolParam(description = "Resource ID ARM completo della risorsa da monitorare") String resourceId,
            @ToolParam(description = "Nome della diagnostic setting da creare") String settingName,
            @ToolParam(description = "Resource ID del workspace Log Analytics di destinazione") String workspaceId) {
        // Invia tutte le categorie disponibili (Azure le filtra automaticamente per tipo risorsa)
        Map<String, Object> body = Map.of(
                "properties", Map.of(
                        "workspaceId", workspaceId,
                        "logs", List.of(
                                Map.of("categoryGroup", "allLogs", "enabled", true),
                                Map.of("categoryGroup", "audit", "enabled", true)
                        ),
                        "metrics", List.of(
                                Map.of("category", "AllMetrics", "enabled", true)
                        )
                )
        );
        return webClient.put()
                .uri("https://management.azure.com" + resourceId + DIAG_PROVIDER + "/" + settingName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione diagnostic settings: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_diagnostic_settings",
          description = "Elimina una diagnostic setting da una risorsa ARM")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteDiagnosticSettings(
            @ToolParam(description = "Resource ID ARM completo della risorsa") String resourceId,
            @ToolParam(description = "Nome della diagnostic setting da eliminare") String settingName) {
        return webClient.delete()
                .uri("https://management.azure.com" + resourceId + DIAG_PROVIDER + "/" + settingName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminato: " + settingName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione diagnostic settings: " + e.getMessage())));
    }
}
