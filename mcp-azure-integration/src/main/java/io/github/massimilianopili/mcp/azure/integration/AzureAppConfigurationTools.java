package io.github.massimilianopili.mcp.azure.integration;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureAppConfigurationTools {

    private static final String API = "2024-05-01";
    private static final String P   = "Microsoft.AppConfiguration/configurationStores";

    private final WebClient w;
    private final AzureProperties props;

    public AzureAppConfigurationTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_app_configurations",
          description = "Elenca tutti gli App Configuration store Azure nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAppConfigurations() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(s -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", s.getOrDefault("name", ""));
                        r.put("location", s.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) s.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) s.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("endpoint", p.getOrDefault("endpoint", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_app_configuration",
          description = "Recupera i dettagli di un App Configuration store")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAppConfiguration(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'App Configuration store") String storeName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + storeName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_app_configuration_keys",
          description = "Elenca le access key di un App Configuration store")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAppConfigurationKeys(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'App Configuration store") String storeName) {
        return w.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + storeName + "/listKeys?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> keys = (List<Map<String, Object>>) res.get("value");
                    return keys.stream().map(k -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", k.getOrDefault("name", ""));
                        r.put("readOnly", k.getOrDefault("readOnly", false));
                        r.put("connectionString", k.getOrDefault("connectionString", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
