package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureMonitorTools {

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureMonitorTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_log_analytics_workspaces",
          description = "Elenca tutti i workspace Log Analytics nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listLogAnalyticsWorkspaces() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.OperationalInsights/workspaces?api-version=2023-09-01")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> wss = (List<Map<String, Object>>) response.get("value");
                    return wss.stream().map(ws -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ws.getOrDefault("name", ""));
                        r.put("location", ws.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) ws.getOrDefault("properties", Map.of());
                        r.put("customerId", p.getOrDefault("customerId", ""));
                        r.put("retentionInDays", p.getOrDefault("retentionInDays", ""));
                        r.put("sku", ((Map<String, Object>) p.getOrDefault("sku", Map.of())).getOrDefault("name", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Log Analytics workspaces: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_log_analytics_workspace",
          description = "Recupera i dettagli di un workspace Log Analytics Azure (incluso customerId per le query)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getLogAnalyticsWorkspace(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del workspace Log Analytics") String workspaceName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.OperationalInsights/workspaces/" + workspaceName + "?api-version=2023-09-01")
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero workspace Log Analytics: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_log_analytics_workspace",
          description = "Crea un workspace Log Analytics Azure per la raccolta di log e metriche")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createLogAnalyticsWorkspace(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del workspace") String workspaceName,
            @ToolParam(description = "Regione Azure, es: westeurope") String location,
            @ToolParam(description = "Giorni di retention (30-730)") int retentionInDays) {
        Map<String, Object> body = Map.of(
                "location", location,
                "properties", Map.of(
                        "retentionInDays", retentionInDays,
                        "sku", Map.of("name", "PerGB2018")
                )
        );
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.OperationalInsights/workspaces/" + workspaceName + "?api-version=2023-09-01")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione workspace Log Analytics: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_metric_alerts",
          description = "Elenca le regole di alert sulle metriche nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listMetricAlerts() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Insights/metricAlerts?api-version=2018-03-01")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> alerts = (List<Map<String, Object>>) response.get("value");
                    return alerts.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", a.getOrDefault("name", ""));
                        r.put("location", a.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) a.getOrDefault("properties", Map.of());
                        r.put("severity", p.getOrDefault("severity", ""));
                        r.put("enabled", p.getOrDefault("enabled", false));
                        r.put("description", p.getOrDefault("description", ""));
                        r.put("evaluationFrequency", p.getOrDefault("evaluationFrequency", ""));
                        r.put("windowSize", p.getOrDefault("windowSize", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista metric alerts: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_action_groups",
          description = "Elenca gli action group Azure Monitor (destinatari notifiche: email, SMS, webhook)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listActionGroups() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Insights/actionGroups?api-version=2023-01-01")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> groups = (List<Map<String, Object>>) response.get("value");
                    return groups.stream().map(g -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", g.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) g.getOrDefault("properties", Map.of());
                        r.put("groupShortName", p.getOrDefault("groupShortName", ""));
                        r.put("enabled", p.getOrDefault("enabled", false));
                        r.put("emailReceivers", ((List<?>) p.getOrDefault("emailReceivers", List.of())).size());
                        r.put("smsReceivers", ((List<?>) p.getOrDefault("smsReceivers", List.of())).size());
                        r.put("webhookReceivers", ((List<?>) p.getOrDefault("webhookReceivers", List.of())).size());
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista action groups: " + e.getMessage()))));
    }
}
