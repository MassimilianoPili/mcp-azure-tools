package io.github.massimilianopili.mcp.azure.compute;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureStaticWebAppsTools {

    private static final String API_VERSION = "2023-12-01";
    private static final String PROVIDER    = "/providers/Microsoft.Web/staticSites";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureStaticWebAppsTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_static_web_apps",
          description = "Elenca tutte le Azure Static Web App nella subscription (hosting statico con CDN integrato)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listStaticWebApps() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> apps = (List<Map<String, Object>>) response.get("value");
                    return apps.stream().map(app -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", app.getOrDefault("name", ""));
                        r.put("location", app.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) app.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) app.getOrDefault("properties", Map.of());
                        r.put("defaultHostname", p.getOrDefault("defaultHostname", ""));
                        r.put("repositoryUrl", p.getOrDefault("repositoryUrl", ""));
                        r.put("branch", p.getOrDefault("branch", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Static Web Apps: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_static_web_app",
          description = "Recupera i dettagli di una Azure Static Web App")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getStaticWebApp(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Static Web App") String appName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Static Web App: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_static_web_app_custom_domains",
          description = "Elenca i domini custom configurati per una Azure Static Web App")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listStaticWebAppCustomDomains(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Static Web App") String appName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName + "/customDomains?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> domains = (List<Map<String, Object>>) response.get("value");
                    return domains.stream().map(d -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", d.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) d.getOrDefault("properties", Map.of());
                        r.put("domainName", p.getOrDefault("domainName", ""));
                        r.put("status", p.getOrDefault("status", ""));
                        r.put("validationToken", p.getOrDefault("validationToken", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista custom domains Static Web App: " + e.getMessage()))));
    }
}
