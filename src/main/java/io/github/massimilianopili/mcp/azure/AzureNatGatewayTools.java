package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureNatGatewayTools {

    private static final String API_VERSION = "2024-03-01";
    private static final String PROVIDER    = "/providers/Microsoft.Network/natGateways";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureNatGatewayTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_nat_gateways",
          description = "Elenca tutti i NAT Gateway nella subscription Azure (IP di uscita fisso per subnet)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listNatGateways() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> gws = (List<Map<String, Object>>) response.get("value");
                    return gws.stream().map(gw -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", gw.getOrDefault("name", ""));
                        r.put("location", gw.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) gw.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) gw.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("idleTimeoutInMinutes", p.getOrDefault("idleTimeoutInMinutes", 4));
                        List<Map<String, Object>> ips = (List<Map<String, Object>>) p.getOrDefault("publicIpAddresses", List.of());
                        r.put("publicIpCount", ips.size());
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista NAT Gateway: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_nat_gateway",
          description = "Recupera i dettagli di un NAT Gateway Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getNatGateway(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del NAT Gateway") String gatewayName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + gatewayName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero NAT Gateway: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_nat_gateway",
          description = "Crea un NAT Gateway Azure per garantire un IP di uscita fisso alle subnet")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createNatGateway(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del NAT Gateway") String gatewayName,
            @ToolParam(description = "Regione Azure, es: westeurope") String location,
            @ToolParam(description = "Resource ID del Public IP Address da associare") String publicIpId) {
        Map<String, Object> body = Map.of(
                "location", location,
                "sku", Map.of("name", "Standard"),
                "properties", Map.of(
                        "idleTimeoutInMinutes", 4,
                        "publicIpAddresses", List.of(Map.of("id", publicIpId))
                )
        );
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + gatewayName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione NAT Gateway: " + e.getMessage())));
    }
}
