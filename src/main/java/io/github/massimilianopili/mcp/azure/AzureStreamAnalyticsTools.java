package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureStreamAnalyticsTools {

    private static final String API = "2021-10-01-preview";
    private static final String P   = "Microsoft.StreamAnalytics/streamingjobs";

    private final WebClient w;
    private final AzureProperties props;

    public AzureStreamAnalyticsTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_stream_analytics_jobs",
          description = "Elenca tutti i job Azure Stream Analytics nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listStreamAnalyticsJobs() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(j -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", j.getOrDefault("name", ""));
                        r.put("location", j.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) j.getOrDefault("properties", Map.of());
                        r.put("jobState", p.getOrDefault("jobState", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("outputStartMode", p.getOrDefault("outputStartMode", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_stream_analytics_job",
          description = "Recupera i dettagli di un job Azure Stream Analytics")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getStreamAnalyticsJob(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del job") String jobName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + jobName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_start_stream_analytics_job",
          description = "Avvia un job Azure Stream Analytics")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> startStreamAnalyticsJob(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del job") String jobName) {
        return w.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + jobName + "/start?api-version=" + API)
                .bodyValue(Map.of("outputStartMode", "JobStartTime"))
                .retrieve().bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Avvio richiesto per " + jobName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_stop_stream_analytics_job",
          description = "Ferma un job Azure Stream Analytics")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> stopStreamAnalyticsJob(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del job") String jobName) {
        return w.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + jobName + "/stop?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Stop richiesto per " + jobName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
