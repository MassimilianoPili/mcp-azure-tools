package io.github.massimilianopili.mcp.azure.monitoring;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureAutoscaleTools {

    private static final String API = "2022-10-01";
    private static final String P   = "Microsoft.Insights/autoscaleSettings";

    private final WebClient w;
    private final AzureProperties props;

    public AzureAutoscaleTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_autoscale_settings",
          description = "Elenca tutte le impostazioni di autoscale nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAutoscaleSettings() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(as -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", as.getOrDefault("name", ""));
                        r.put("location", as.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) as.getOrDefault("properties", Map.of());
                        r.put("enabled", p.getOrDefault("enabled", true));
                        r.put("targetResourceUri", p.getOrDefault("targetResourceUri", ""));
                        List<?> profiles = (List<?>) p.getOrDefault("profiles", List.of());
                        r.put("profileCount", profiles.size());
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_autoscale_setting",
          description = "Recupera i dettagli di una impostazione di autoscale")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAutoscaleSetting(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'impostazione autoscale") String autoscaleSettingName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + autoscaleSettingName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_autoscale_setting",
          description = "Elimina una impostazione di autoscale")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteAutoscaleSetting(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'impostazione autoscale da eliminare") String autoscaleSettingName) {
        return w.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + autoscaleSettingName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminazione avviata per " + autoscaleSettingName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
