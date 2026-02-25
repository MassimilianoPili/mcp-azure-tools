package io.github.massimilianopili.mcp.azure.network;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureExpressRouteTools {

    private static final String API = "2024-03-01";
    private static final String P   = "Microsoft.Network/expressRouteCircuits";

    private final WebClient w;
    private final AzureProperties props;

    public AzureExpressRouteTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_expressroute_circuits",
          description = "Elenca tutti i circuiti ExpressRoute nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listExpressRouteCircuits() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(c -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", c.getOrDefault("name", ""));
                        r.put("location", c.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) c.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        r.put("tier", sku.getOrDefault("tier", ""));
                        Map<String, Object> p = (Map<String, Object>) c.getOrDefault("properties", Map.of());
                        r.put("circuitProvisioningState", p.getOrDefault("circuitProvisioningState", ""));
                        r.put("serviceProviderProvisioningState", p.getOrDefault("serviceProviderProvisioningState", ""));
                        Map<String, Object> sp = (Map<String, Object>) p.getOrDefault("serviceProviderProperties", Map.of());
                        r.put("bandwidthInMbps", sp.getOrDefault("bandwidthInMbps", 0));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_expressroute_circuit",
          description = "Recupera i dettagli di un circuito ExpressRoute")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getExpressRouteCircuit(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del circuito ExpressRoute") String circuitName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + circuitName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_expressroute_peerings",
          description = "Elenca i peering di un circuito ExpressRoute")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listExpressRoutePeerings(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del circuito ExpressRoute") String circuitName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + circuitName + "/peerings?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(peer -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", peer.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) peer.getOrDefault("properties", Map.of());
                        r.put("peeringType", p.getOrDefault("peeringType", ""));
                        r.put("state", p.getOrDefault("state", ""));
                        r.put("peerASN", p.getOrDefault("peerASN", ""));
                        r.put("primaryPeerAddressPrefix", p.getOrDefault("primaryPeerAddressPrefix", ""));
                        r.put("secondaryPeerAddressPrefix", p.getOrDefault("secondaryPeerAddressPrefix", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
