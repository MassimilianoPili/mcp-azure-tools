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
public class AzureBastionTools {

    private static final String API_VERSION = "2024-03-01";
    private static final String PROVIDER    = "/providers/Microsoft.Network/bastionHosts";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureBastionTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_bastions",
          description = "Elenca tutti gli Azure Bastion Host nella subscription (accesso SSH/RDP sicuro senza IP pubblico sulle VM)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listBastions() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> hosts = (List<Map<String, Object>>) response.get("value");
                    return hosts.stream().map(h -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", h.getOrDefault("name", ""));
                        r.put("location", h.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) h.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("scaleUnits", p.getOrDefault("scaleUnits", ""));
                        r.put("sku", ((Map<String, Object>) h.getOrDefault("sku", Map.of())).getOrDefault("name", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Bastion hosts: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_bastion",
          description = "Recupera i dettagli di un Azure Bastion Host")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getBastion(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del Bastion Host") String bastionName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + bastionName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Bastion: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_bastion",
          description = "Crea un Azure Bastion Host in una subnet AzureBastionSubnet di una VNet")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createBastion(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del Bastion Host da creare") String bastionName,
            @ToolParam(description = "Regione Azure, es: westeurope") String location,
            @ToolParam(description = "Resource ID della subnet AzureBastionSubnet") String subnetId,
            @ToolParam(description = "Resource ID del Public IP Address da associare") String publicIpId) {
        Map<String, Object> body = Map.of(
                "location", location,
                "sku", Map.of("name", "Standard"),
                "properties", Map.of(
                        "ipConfigurations", List.of(Map.of(
                                "name", "IpConf",
                                "properties", Map.of(
                                        "subnet", Map.of("id", subnetId),
                                        "publicIPAddress", Map.of("id", publicIpId)
                                )
                        ))
                )
        );
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + bastionName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione Bastion: " + e.getMessage())));
    }
}
