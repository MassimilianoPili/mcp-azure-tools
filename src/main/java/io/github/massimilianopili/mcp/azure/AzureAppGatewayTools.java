package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureAppGatewayTools {

    private static final String API_VERSION = "2024-03-01";
    private static final String PROVIDER    = "/providers/Microsoft.Network/applicationGateways";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureAppGatewayTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_app_gateways",
          description = "Elenca tutti gli Application Gateway nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAppGateways() {
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
                        Map<String, Object> p = (Map<String, Object>) gw.getOrDefault("properties", Map.of());
                        Map<String, Object> sku = (Map<String, Object>) p.getOrDefault("sku", Map.of());
                        r.put("tier", sku.getOrDefault("tier", ""));
                        r.put("capacity", sku.getOrDefault("capacity", ""));
                        r.put("operationalState", p.getOrDefault("operationalState", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Application Gateway: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_app_gateway",
          description = "Recupera i dettagli di un Application Gateway Azure (routing rules, backends, listeners)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAppGateway(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'Application Gateway") String gatewayName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + gatewayName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Application Gateway: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_start_app_gateway",
          description = "Avvia un Application Gateway Azure (operazione asincrona)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> startAppGateway(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'Application Gateway") String gatewayName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + gatewayName + "/start?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Start avviato per " + gatewayName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore avvio Application Gateway: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_stop_app_gateway",
          description = "Ferma e dealloca un Application Gateway Azure (operazione asincrona)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> stopAppGateway(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'Application Gateway") String gatewayName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + gatewayName + "/stop?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Stop avviato per " + gatewayName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore stop Application Gateway: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_get_app_gateway_backend_health",
          description = "Verifica la salute dei backend pool di un Application Gateway Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAppGatewayBackendHealth(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'Application Gateway") String gatewayName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + gatewayName + "/backendhealth?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore backend health Application Gateway: " + e.getMessage())));
    }
}
