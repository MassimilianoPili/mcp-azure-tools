package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureAlertTools {

    private static final String METRIC_API   = "2018-03-01";
    private static final String ACTIVITY_API = "2020-10-01";

    private final WebClient w;
    private final AzureProperties props;

    public AzureAlertTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_metric_alerts",
          description = "Elenca tutte le regole di alert su metriche nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listMetricAlerts() {
        return w.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Insights/metricAlerts?api-version=" + METRIC_API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", a.getOrDefault("name", ""));
                        r.put("location", a.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) a.getOrDefault("properties", Map.of());
                        r.put("enabled", p.getOrDefault("enabled", true));
                        r.put("severity", p.getOrDefault("severity", ""));
                        r.put("description", p.getOrDefault("description", ""));
                        r.put("scopes", p.getOrDefault("scopes", List.of()));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_metric_alert",
          description = "Recupera i dettagli di una regola di alert su metriche")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getMetricAlert(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della regola di alert") String ruleName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/Microsoft.Insights/metricAlerts/" + ruleName + "?api-version=" + METRIC_API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_activity_log_alerts",
          description = "Elenca le regole di alert sull'activity log nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listActivityLogAlerts() {
        return w.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Insights/activityLogAlerts?api-version=" + ACTIVITY_API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", a.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) a.getOrDefault("properties", Map.of());
                        r.put("enabled", p.getOrDefault("enabled", true));
                        r.put("description", p.getOrDefault("description", ""));
                        r.put("scopes", p.getOrDefault("scopes", List.of()));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_delete_metric_alert",
          description = "Elimina una regola di alert su metriche")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteMetricAlert(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della regola di alert da eliminare") String ruleName) {
        return w.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/Microsoft.Insights/metricAlerts/" + ruleName + "?api-version=" + METRIC_API)
                .retrieve().bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminazione avviata per " + ruleName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
