package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureLoadBalancerTools {

    private static final String API = "2024-03-01";
    private static final String P   = "Microsoft.Network/loadBalancers";

    private final WebClient w;
    private final AzureProperties props;

    public AzureLoadBalancerTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_load_balancers",
          description = "Elenca tutti i load balancer Azure nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listLoadBalancers() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(lb -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", lb.getOrDefault("name", ""));
                        r.put("location", lb.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) lb.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) lb.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        List<?> feIps = (List<?>) p.getOrDefault("frontendIPConfigurations", List.of());
                        r.put("frontendCount", feIps.size());
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_load_balancer",
          description = "Recupera i dettagli di un load balancer Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getLoadBalancer(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del load balancer") String lbName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + lbName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_load_balancer_rules",
          description = "Elenca le regole di bilanciamento di un load balancer")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listLoadBalancerRules(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del load balancer") String lbName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + lbName + "/loadBalancingRules?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(rule -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", rule.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) rule.getOrDefault("properties", Map.of());
                        r.put("protocol", p.getOrDefault("protocol", ""));
                        r.put("frontendPort", p.getOrDefault("frontendPort", 0));
                        r.put("backendPort", p.getOrDefault("backendPort", 0));
                        r.put("loadDistribution", p.getOrDefault("loadDistribution", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_delete_load_balancer",
          description = "Elimina un load balancer Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteLoadBalancer(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del load balancer da eliminare") String lbName) {
        return w.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + lbName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminazione avviata per " + lbName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
