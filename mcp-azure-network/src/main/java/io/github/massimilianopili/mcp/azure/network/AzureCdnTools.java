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
public class AzureCdnTools {

    private static final String API = "2024-02-01";
    private static final String P   = "Microsoft.Cdn/profiles";
    private static final Set<String> CDN_SKUS = Set.of(
            "Standard_Microsoft", "Standard_Verizon", "Premium_Verizon",
            "Standard_Akamai", "Standard_ChinaCdn");

    private final WebClient w;
    private final AzureProperties props;

    public AzureCdnTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_cdn_profiles",
          description = "Elenca i profili CDN classici (esclusi Front Door) nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listCdnProfiles() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream()
                            .filter(p -> {
                                Map<String, Object> sku = (Map<String, Object>) p.getOrDefault("sku", Map.of());
                                return CDN_SKUS.contains(sku.getOrDefault("name", ""));
                            })
                            .map(profile -> {
                                Map<String, Object> r = new LinkedHashMap<>();
                                r.put("name", profile.getOrDefault("name", ""));
                                r.put("location", profile.getOrDefault("location", ""));
                                Map<String, Object> sku = (Map<String, Object>) profile.getOrDefault("sku", Map.of());
                                r.put("sku", sku.getOrDefault("name", ""));
                                Map<String, Object> pp = (Map<String, Object>) profile.getOrDefault("properties", Map.of());
                                r.put("provisioningState", pp.getOrDefault("provisioningState", ""));
                                r.put("resourceState", pp.getOrDefault("resourceState", ""));
                                return r;
                            }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_cdn_profile",
          description = "Recupera i dettagli di un profilo CDN")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getCdnProfile(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del profilo CDN") String profileName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + profileName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_cdn_endpoints",
          description = "Elenca gli endpoint di un profilo CDN")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listCdnEndpoints(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del profilo CDN") String profileName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + profileName + "/endpoints?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(ep -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ep.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) ep.getOrDefault("properties", Map.of());
                        r.put("hostName", p.getOrDefault("hostName", ""));
                        r.put("resourceState", p.getOrDefault("resourceState", ""));
                        r.put("isHttpsAllowed", p.getOrDefault("isHttpsAllowed", true));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
