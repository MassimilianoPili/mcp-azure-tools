package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureFirewallTools {

    private static final String API_VERSION          = "2024-03-01";
    private static final String FIREWALL_PROVIDER    = "/providers/Microsoft.Network/azureFirewalls";
    private static final String POLICY_PROVIDER      = "/providers/Microsoft.Network/firewallPolicies";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureFirewallTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_firewalls",
          description = "Elenca tutti gli Azure Firewall nella subscription (firewall L4/L7 gestito)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listFirewalls() {
        return webClient.get()
                .uri(props.getArmBase() + FIREWALL_PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> fws = (List<Map<String, Object>>) response.get("value");
                    return fws.stream().map(fw -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", fw.getOrDefault("name", ""));
                        r.put("location", fw.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) fw.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("threatIntelMode", p.getOrDefault("threatIntelMode", ""));
                        r.put("sku", ((Map<String, Object>) p.getOrDefault("sku", Map.of())).getOrDefault("tier", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Azure Firewall: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_firewall",
          description = "Recupera i dettagli di un Azure Firewall (regole NAT, network, application)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getFirewall(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'Azure Firewall") String firewallName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + FIREWALL_PROVIDER + "/" + firewallName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Azure Firewall: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_firewall_policies",
          description = "Elenca le Firewall Policy nella subscription Azure (regole IDPS, TLS inspection, rule collections)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listFirewallPolicies() {
        return webClient.get()
                .uri(props.getArmBase() + POLICY_PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> policies = (List<Map<String, Object>>) response.get("value");
                    return policies.stream().map(pol -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", pol.getOrDefault("name", ""));
                        r.put("location", pol.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) pol.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("threatIntelMode", p.getOrDefault("threatIntelMode", ""));
                        r.put("tier", ((Map<String, Object>) p.getOrDefault("sku", Map.of())).getOrDefault("tier", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Firewall Policy: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_firewall_policy",
          description = "Recupera i dettagli di una Firewall Policy Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getFirewallPolicy(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Firewall Policy") String policyName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + POLICY_PROVIDER + "/" + policyName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Firewall Policy: " + e.getMessage())));
    }
}
