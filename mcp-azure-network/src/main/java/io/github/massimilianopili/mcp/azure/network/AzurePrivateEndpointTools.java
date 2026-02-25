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
public class AzurePrivateEndpointTools {

    private static final String API_VERSION = "2024-03-01";
    private static final String PROVIDER    = "/providers/Microsoft.Network/privateEndpoints";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzurePrivateEndpointTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_private_endpoints",
          description = "Elenca tutti i Private Endpoint nella subscription Azure (connessioni private a servizi PaaS)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPrivateEndpoints() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> eps = (List<Map<String, Object>>) response.get("value");
                    return eps.stream().map(ep -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ep.getOrDefault("name", ""));
                        r.put("location", ep.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) ep.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        List<Map<String, Object>> conns = (List<Map<String, Object>>) p.getOrDefault("privateLinkServiceConnections", List.of());
                        if (!conns.isEmpty()) {
                            Map<String, Object> conn = conns.get(0);
                            Map<String, Object> cp = (Map<String, Object>) conn.getOrDefault("properties", Map.of());
                            r.put("linkedService", cp.getOrDefault("privateLinkServiceId", ""));
                            r.put("groupIds", cp.getOrDefault("groupIds", List.of()));
                        }
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista private endpoints: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_private_endpoint",
          description = "Recupera i dettagli di un Private Endpoint Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getPrivateEndpoint(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del private endpoint") String endpointName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + endpointName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero private endpoint: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_private_endpoint",
          description = "Crea un Private Endpoint Azure per connettere un servizio PaaS alla VNet senza IP pubblico")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createPrivateEndpoint(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del private endpoint") String endpointName,
            @ToolParam(description = "Regione Azure, es: westeurope") String location,
            @ToolParam(description = "Resource ID della subnet in cui creare il PE") String subnetId,
            @ToolParam(description = "Resource ID del servizio PaaS da collegare (es: Azure SQL, PostgreSQL, Key Vault)") String serviceId,
            @ToolParam(description = "Group ID del servizio, es: sqlServer, postgresqlServer, vault") String groupId) {
        Map<String, Object> body = Map.of(
                "location", location,
                "properties", Map.of(
                        "subnet", Map.of("id", subnetId),
                        "privateLinkServiceConnections", List.of(Map.of(
                                "name", endpointName + "-conn",
                                "properties", Map.of(
                                        "privateLinkServiceId", serviceId,
                                        "groupIds", List.of(groupId)
                                )
                        ))
                )
        );
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + endpointName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione private endpoint: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_private_endpoint",
          description = "Elimina un Private Endpoint Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deletePrivateEndpoint(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del private endpoint da eliminare") String endpointName) {
        return webClient.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + endpointName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminato: " + endpointName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione private endpoint: " + e.getMessage())));
    }
}
