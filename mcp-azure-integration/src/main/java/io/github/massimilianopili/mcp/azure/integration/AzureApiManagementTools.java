package io.github.massimilianopili.mcp.azure.integration;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureApiManagementTools {

    private static final String API_VERSION = "2023-09-01-preview";
    private static final String PROVIDER    = "/providers/Microsoft.ApiManagement/service";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureApiManagementTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_apim_services",
          description = "Elenca tutti i servizi Azure API Management (APIM) nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listApimServices() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> services = (List<Map<String, Object>>) response.get("value");
                    return services.stream().map(s -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", s.getOrDefault("name", ""));
                        r.put("location", s.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) s.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) s.getOrDefault("properties", Map.of());
                        r.put("gatewayUrl", p.getOrDefault("gatewayUrl", ""));
                        r.put("portalUrl", p.getOrDefault("portalUrl", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista APIM services: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_apim_service",
          description = "Recupera i dettagli di un servizio Azure API Management")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getApimService(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio APIM") String serviceName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serviceName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero APIM service: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_apim_apis",
          description = "Elenca le API pubblicate in un servizio Azure API Management")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listApimApis(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio APIM") String serviceName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serviceName + "/apis?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> apis = (List<Map<String, Object>>) response.get("value");
                    return apis.stream().map(api -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", api.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) api.getOrDefault("properties", Map.of());
                        r.put("displayName", p.getOrDefault("displayName", ""));
                        r.put("path", p.getOrDefault("path", ""));
                        r.put("protocols", p.getOrDefault("protocols", List.of()));
                        r.put("serviceUrl", p.getOrDefault("serviceUrl", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista APIM APIs: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_apim_products",
          description = "Elenca i prodotti di un servizio Azure API Management (gruppi di API con subscription)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listApimProducts(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio APIM") String serviceName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serviceName + "/products?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> products = (List<Map<String, Object>>) response.get("value");
                    return products.stream().map(prod -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", prod.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) prod.getOrDefault("properties", Map.of());
                        r.put("displayName", p.getOrDefault("displayName", ""));
                        r.put("state", p.getOrDefault("state", ""));
                        r.put("subscriptionRequired", p.getOrDefault("subscriptionRequired", true));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista APIM products: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_apim_subscriptions",
          description = "Elenca le subscription attive di un servizio Azure API Management")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listApimSubscriptions(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del servizio APIM") String serviceName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serviceName + "/subscriptions?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> subs = (List<Map<String, Object>>) response.get("value");
                    return subs.stream().map(sub -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", sub.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) sub.getOrDefault("properties", Map.of());
                        r.put("displayName", p.getOrDefault("displayName", ""));
                        r.put("state", p.getOrDefault("state", ""));
                        r.put("scope", p.getOrDefault("scope", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista APIM subscriptions: " + e.getMessage()))));
    }
}
