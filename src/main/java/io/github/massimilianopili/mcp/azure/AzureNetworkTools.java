package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureNetworkTools {

    private static final String API_VERSION = "2024-03-01";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureNetworkTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_vnets",
          description = "Elenca tutte le Virtual Network (VNet) nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listVnets() {
        return listResources("/providers/Microsoft.Network/virtualNetworks", "VNet");
    }

    @ReactiveTool(name = "azure_get_vnet",
          description = "Recupera i dettagli di una Virtual Network Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getVnet(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della VNet") String vnetName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.Network/virtualNetworks/" + vnetName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero VNet: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_subnets",
          description = "Elenca le subnet di una Virtual Network Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSubnets(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della VNet") String vnetName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.Network/virtualNetworks/" + vnetName + "/subnets?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> subnets = (List<Map<String, Object>>) response.get("value");
                    return subnets.stream().map(s -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", s.getOrDefault("name", ""));
                        r.put("addressPrefix", s.containsKey("properties")
                                ? ((Map<String, Object>) s.get("properties")).getOrDefault("addressPrefix", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista subnet: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_nsgs",
          description = "Elenca tutti i Network Security Group (NSG) nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listNsgs() {
        return listResources("/providers/Microsoft.Network/networkSecurityGroups", "NSG");
    }

    @ReactiveTool(name = "azure_list_public_ips",
          description = "Elenca tutti gli indirizzi IP pubblici nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPublicIps() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Network/publicIPAddresses?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> ips = (List<Map<String, Object>>) response.get("value");
                    return ips.stream().map(ip -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ip.getOrDefault("name", ""));
                        r.put("location", ip.getOrDefault("location", ""));
                        r.put("ipAddress", ip.containsKey("properties")
                                ? ((Map<String, Object>) ip.get("properties")).getOrDefault("ipAddress", "")
                                : "");
                        r.put("allocationMethod", ip.containsKey("properties")
                                ? ((Map<String, Object>) ip.get("properties")).getOrDefault("publicIPAllocationMethod", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista IP pubblici: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_load_balancers",
          description = "Elenca tutti i Load Balancer nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listLoadBalancers() {
        return listResources("/providers/Microsoft.Network/loadBalancers", "Load Balancer");
    }

    @ReactiveTool(name = "azure_list_network_interfaces",
          description = "Elenca tutte le Network Interface (NIC) nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listNetworkInterfaces() {
        return listResources("/providers/Microsoft.Network/networkInterfaces", "NIC");
    }

    @SuppressWarnings("unchecked")
    private Mono<List<Map<String, Object>>> listResources(String providerPath, String resourceType) {
        return webClient.get()
                .uri(props.getArmBase() + providerPath + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
                    return items.stream().map(item -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", item.getOrDefault("name", ""));
                        r.put("location", item.getOrDefault("location", ""));
                        r.put("resourceGroup", extractRgFromId((String) item.getOrDefault("id", "")));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista " + resourceType + ": " + e.getMessage()))));
    }

    private String extractRgFromId(String id) {
        if (id == null || id.isEmpty()) return "";
        String[] parts = id.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("resourceGroups".equalsIgnoreCase(parts[i])) return parts[i + 1];
        }
        return "";
    }
}
