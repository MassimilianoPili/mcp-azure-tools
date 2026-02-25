package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureVpnGatewayTools {

    private static final String API = "2024-03-01";
    private static final String P   = "Microsoft.Network/virtualNetworkGateways";

    private final WebClient w;
    private final AzureProperties props;

    public AzureVpnGatewayTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_vpn_gateways",
          description = "Elenca tutti i VPN Gateway (Virtual Network Gateway) in un resource group")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listVpnGateways(
            @ToolParam(description = "Nome del resource group") String resourceGroup) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(gw -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", gw.getOrDefault("name", ""));
                        r.put("location", gw.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) gw.getOrDefault("properties", Map.of());
                        r.put("gatewayType", p.getOrDefault("gatewayType", ""));
                        r.put("vpnType", p.getOrDefault("vpnType", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        Map<String, Object> sku = (Map<String, Object>) p.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_vpn_gateway",
          description = "Recupera i dettagli di un VPN Gateway")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getVpnGateway(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del gateway") String gatewayName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + gatewayName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_vpn_connections",
          description = "Elenca le connessioni VPN di un resource group")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listVpnConnections(
            @ToolParam(description = "Nome del resource group") String resourceGroup) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/Microsoft.Network/connections?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(conn -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", conn.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) conn.getOrDefault("properties", Map.of());
                        r.put("connectionType", p.getOrDefault("connectionType", ""));
                        r.put("connectionStatus", p.getOrDefault("connectionStatus", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("routingWeight", p.getOrDefault("routingWeight", 0));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
