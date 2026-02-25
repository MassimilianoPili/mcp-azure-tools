package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureIotHubTools {

    private static final String API = "2023-06-30";
    private static final String P   = "Microsoft.Devices/IotHubs";

    private final WebClient w;
    private final AzureProperties props;

    public AzureIotHubTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_iot_hubs",
          description = "Elenca tutti gli IoT Hub Azure nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listIotHubs() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(h -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", h.getOrDefault("name", ""));
                        r.put("location", h.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) h.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        r.put("capacity", sku.getOrDefault("capacity", 1));
                        Map<String, Object> p = (Map<String, Object>) h.getOrDefault("properties", Map.of());
                        r.put("state", p.getOrDefault("state", ""));
                        r.put("hostName", p.getOrDefault("hostName", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_iot_hub",
          description = "Recupera i dettagli di un IoT Hub Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getIotHub(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'IoT Hub") String iothubName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + iothubName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_iot_hub_consumer_groups",
          description = "Elenca i consumer group dell'endpoint Event Hub built-in di un IoT Hub")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listIotHubConsumerGroups(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'IoT Hub") String iothubName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + iothubName + "/eventHubEndpoints/events/ConsumerGroups?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(cg -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", cg.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) cg.getOrDefault("properties", Map.of());
                        r.put("created", p.getOrDefault("created", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
