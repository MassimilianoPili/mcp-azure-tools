package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureSpringAppsTools {

    private static final String API = "2024-01-01-preview";
    private static final String P   = "Microsoft.AppPlatform/Spring";

    private final WebClient w;
    private final AzureProperties props;

    public AzureSpringAppsTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_spring_services",
          description = "Elenca tutti i servizi Azure Spring Apps nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSpringServices() {
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
                        r.put("fqdn", p.getOrDefault("fqdn", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_spring_service",
          description = "Recupera i dettagli di un servizio Azure Spring Apps")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getSpringService(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio Spring Apps") String serviceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + serviceName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_spring_apps",
          description = "Elenca le app di un servizio Azure Spring Apps")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSpringApps(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio Spring Apps") String serviceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + serviceName + "/apps?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(app -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", app.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) app.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("public", p.getOrDefault("public", false));
                        r.put("url", p.getOrDefault("url", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
