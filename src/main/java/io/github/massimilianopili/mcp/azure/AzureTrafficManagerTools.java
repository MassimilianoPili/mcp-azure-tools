package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureTrafficManagerTools {

    private static final String API = "2022-04-01";
    private static final String P   = "Microsoft.Network/trafficmanagerprofiles";

    private final WebClient w;
    private final AzureProperties props;

    public AzureTrafficManagerTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_traffic_manager_profiles",
          description = "Elenca tutti i profili Traffic Manager nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listTrafficManagerProfiles() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(tm -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", tm.getOrDefault("name", ""));
                        r.put("location", tm.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) tm.getOrDefault("properties", Map.of());
                        r.put("profileStatus", p.getOrDefault("profileStatus", ""));
                        r.put("trafficRoutingMethod", p.getOrDefault("trafficRoutingMethod", ""));
                        Map<String, Object> dns = (Map<String, Object>) p.getOrDefault("dnsConfig", Map.of());
                        r.put("fqdn", dns.getOrDefault("fqdn", ""));
                        List<?> endpoints = (List<?>) p.getOrDefault("endpoints", List.of());
                        r.put("endpointCount", endpoints.size());
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_traffic_manager_profile",
          description = "Recupera i dettagli di un profilo Traffic Manager")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getTrafficManagerProfile(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del profilo Traffic Manager") String profileName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + profileName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_traffic_manager_endpoints",
          description = "Elenca gli endpoint di un profilo Traffic Manager (inclusi nel profilo)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listTrafficManagerEndpoints(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del profilo Traffic Manager") String profileName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + profileName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    Map<String, Object> p = (Map<String, Object>) res.getOrDefault("properties", Map.of());
                    List<Map<String, Object>> endpoints = (List<Map<String, Object>>) p.getOrDefault("endpoints", List.of());
                    return endpoints.stream().map(ep -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ep.getOrDefault("name", ""));
                        r.put("type", ep.getOrDefault("type", ""));
                        Map<String, Object> epP = (Map<String, Object>) ep.getOrDefault("properties", Map.of());
                        r.put("endpointStatus", epP.getOrDefault("endpointStatus", ""));
                        r.put("target", epP.getOrDefault("target", ""));
                        r.put("weight", epP.getOrDefault("weight", 1));
                        r.put("priority", epP.getOrDefault("priority", 1));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_delete_traffic_manager_profile",
          description = "Elimina un profilo Traffic Manager")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteTrafficManagerProfile(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del profilo Traffic Manager da eliminare") String profileName) {
        return w.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + profileName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminazione avviata per " + profileName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
