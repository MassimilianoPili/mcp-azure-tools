package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Tool per Azure Front Door Standard/Premium.
 * Nota: Front Door Standard/Premium usa il provider Microsoft.Cdn/profiles
 * con SKU Standard_AzureFrontDoor o Premium_AzureFrontDoor (non il classico
 * Microsoft.Network/frontDoors, deprecato).
 */
@Service
public class AzureFrontDoorTools {

    private static final String API_VERSION = "2024-02-01";
    private static final String PROVIDER    = "/providers/Microsoft.Cdn/profiles";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureFrontDoorTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_front_door_profiles",
          description = "Elenca i profili Azure Front Door (Standard/Premium) nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listFrontDoorProfiles() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> profiles = (List<Map<String, Object>>) response.get("value");
                    return profiles.stream()
                            .filter(p -> {
                                Map<String, Object> sku = (Map<String, Object>) p.getOrDefault("sku", Map.of());
                                String skuName = (String) sku.getOrDefault("name", "");
                                return skuName.contains("AzureFrontDoor");
                            })
                            .map(p -> {
                                Map<String, Object> r = new LinkedHashMap<>();
                                r.put("name", p.getOrDefault("name", ""));
                                r.put("location", p.getOrDefault("location", ""));
                                Map<String, Object> sku = (Map<String, Object>) p.getOrDefault("sku", Map.of());
                                r.put("sku", sku.getOrDefault("name", ""));
                                Map<String, Object> props2 = (Map<String, Object>) p.getOrDefault("properties", Map.of());
                                r.put("frontDoorId", props2.getOrDefault("frontDoorId", ""));
                                r.put("provisioningState", props2.getOrDefault("provisioningState", ""));
                                return r;
                            }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Front Door profiles: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_front_door_profile",
          description = "Recupera i dettagli di un profilo Azure Front Door")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getFrontDoorProfile(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del profilo Front Door") String profileName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + profileName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Front Door profile: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_front_door_endpoints",
          description = "Elenca gli endpoint di un profilo Azure Front Door (hostName pubblici)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listFrontDoorEndpoints(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del profilo Front Door") String profileName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + profileName + "/afdEndpoints?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> eps = (List<Map<String, Object>>) response.get("value");
                    return eps.stream().map(ep -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ep.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) ep.getOrDefault("properties", Map.of());
                        r.put("hostName", p.getOrDefault("hostName", ""));
                        r.put("enabledState", p.getOrDefault("enabledState", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Front Door endpoints: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_front_door_routes",
          description = "Elenca le route di un endpoint Azure Front Door (mapping path pattern -> origin group)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listFrontDoorRoutes(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del profilo Front Door") String profileName,
            @ToolParam(description = "Nome dell'endpoint AFD") String endpointName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + profileName
                        + "/afdEndpoints/" + endpointName + "/routes?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("value");
                    return routes.stream().map(route -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", route.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) route.getOrDefault("properties", Map.of());
                        r.put("patternsToMatch", p.getOrDefault("patternsToMatch", List.of()));
                        r.put("enabledState", p.getOrDefault("enabledState", ""));
                        r.put("httpsRedirect", p.getOrDefault("httpsRedirect", ""));
                        r.put("forwardingProtocol", p.getOrDefault("forwardingProtocol", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Front Door routes: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_front_door_origin_groups",
          description = "Elenca gli origin group di un profilo Azure Front Door (backend pool con health probe)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listFrontDoorOriginGroups(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del profilo Front Door") String profileName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + profileName + "/originGroups?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> groups = (List<Map<String, Object>>) response.get("value");
                    return groups.stream().map(g -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", g.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) g.getOrDefault("properties", Map.of());
                        r.put("sessionAffinityState", p.getOrDefault("sessionAffinityState", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        Map<String, Object> probe = (Map<String, Object>) p.getOrDefault("healthProbeSettings", Map.of());
                        r.put("probePath", probe.getOrDefault("probePath", ""));
                        r.put("probeProtocol", probe.getOrDefault("probeProtocol", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Front Door origin groups: " + e.getMessage()))));
    }
}
