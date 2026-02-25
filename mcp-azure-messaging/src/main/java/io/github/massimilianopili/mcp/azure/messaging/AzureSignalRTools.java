package io.github.massimilianopili.mcp.azure.messaging;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureSignalRTools {

    private static final String API = "2023-02-01";
    private static final String P   = "Microsoft.SignalRService/signalR";

    private final WebClient w;
    private final AzureProperties props;

    public AzureSignalRTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_signalr_services",
          description = "Elenca tutti i servizi Azure SignalR nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSignalRServices() {
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
                        r.put("capacity", sku.getOrDefault("capacity", 1));
                        Map<String, Object> p = (Map<String, Object>) s.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("hostName", p.getOrDefault("hostName", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_signalr_service",
          description = "Recupera i dettagli di un servizio Azure SignalR")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getSignalRService(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio SignalR") String resourceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + resourceName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_signalr_keys",
          description = "Recupera le chiavi di accesso di un servizio Azure SignalR")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> listSignalRKeys(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio SignalR") String resourceName) {
        return w.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + resourceName + "/listKeys?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
