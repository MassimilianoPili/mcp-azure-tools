package io.github.massimilianopili.mcp.azure.compute;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureFunctionTools {

    private static final String API_VERSION = "2023-12-01";
    private static final String PROVIDER    = "/providers/Microsoft.Web/sites";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureFunctionTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_function_apps",
          description = "Elenca tutte le Function App nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listFunctionApps() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?$filter=kind eq 'functionapp'&api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> apps = (List<Map<String, Object>>) response.get("value");
                    return apps.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", a.getOrDefault("name", ""));
                        r.put("location", a.getOrDefault("location", ""));
                        r.put("state", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("state", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Function App: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_function_app",
          description = "Recupera i dettagli di una Function App Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getFunctionApp(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Function App") String appName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Function App: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_functions",
          description = "Elenca le funzioni all'interno di una Function App Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listFunctions(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Function App") String appName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName
                        + "/functions?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> functions = (List<Map<String, Object>>) response.get("value");
                    return functions.stream().map(f -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", f.getOrDefault("name", ""));
                        r.put("isDisabled", f.containsKey("properties")
                                ? ((Map<String, Object>) f.get("properties")).getOrDefault("isDisabled", false)
                                : false);
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista funzioni: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_function_keys",
          description = "Recupera le chiavi di accesso di una funzione specifica in una Function App Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> listFunctionKeys(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Function App") String appName,
            @ToolParam(description = "Nome della funzione") String functionName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + appName
                        + "/functions/" + functionName + "/listKeys?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero chiavi funzione: " + e.getMessage())));
    }
}
