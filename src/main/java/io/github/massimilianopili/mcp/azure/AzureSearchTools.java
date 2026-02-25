package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureSearchTools {

    private static final String API = "2024-06-01-preview";
    private static final String P   = "Microsoft.Search/searchServices";

    private final WebClient w;
    private final AzureProperties props;

    public AzureSearchTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_search_services",
          description = "Elenca tutti i servizi Azure AI Search nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSearchServices() {
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
                        r.put("status", p.getOrDefault("status", ""));
                        r.put("replicaCount", p.getOrDefault("replicaCount", 1));
                        r.put("partitionCount", p.getOrDefault("partitionCount", 1));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_search_service",
          description = "Recupera i dettagli di un servizio Azure AI Search")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getSearchService(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio di ricerca") String searchServiceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + searchServiceName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_search_admin_keys",
          description = "Recupera le chiavi amministrative di un servizio Azure AI Search")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> listSearchAdminKeys(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio di ricerca") String searchServiceName) {
        return w.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + searchServiceName + "/listAdminKeys?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
