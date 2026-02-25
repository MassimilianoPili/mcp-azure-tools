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
public class AzureAcrTools {

    private static final String API_VERSION = "2023-11-01-preview";
    private static final String PROVIDER    = "/providers/Microsoft.ContainerRegistry/registries";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureAcrTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_container_registries",
          description = "Elenca tutti i Container Registry (ACR) nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listContainerRegistries() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> registries = (List<Map<String, Object>>) response.get("value");
                    return registries.stream().map(reg -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", reg.getOrDefault("name", ""));
                        r.put("location", reg.getOrDefault("location", ""));
                        r.put("loginServer", reg.containsKey("properties")
                                ? ((Map<String, Object>) reg.get("properties")).getOrDefault("loginServer", "")
                                : "");
                        r.put("sku", reg.containsKey("sku") ? ((Map<String, Object>) reg.get("sku")).getOrDefault("name", "") : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista ACR: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_container_registry",
          description = "Recupera i dettagli di un Container Registry Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getContainerRegistry(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del Container Registry") String registryName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + registryName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero ACR: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_acr_repositories",
          description = "Elenca i repository in un Azure Container Registry (Docker Registry API v2)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> listAcrRepositories(
            @ToolParam(description = "Nome del registry (senza .azurecr.io), es: mioregistry") String registryName) {
        return webClient.get()
                .uri("https://" + registryName + ".azurecr.io/v2/_catalog")
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore lista repository ACR: " + e.getMessage()
                        + " (nota: richiede autenticazione specifica ACR, non ARM token)")));
    }

    @ReactiveTool(name = "azure_list_acr_tags",
          description = "Elenca i tag di un repository in un Azure Container Registry")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> listAcrTags(
            @ToolParam(description = "Nome del registry (senza .azurecr.io)") String registryName,
            @ToolParam(description = "Nome del repository, es: myapp/backend") String repositoryName) {
        return webClient.get()
                .uri("https://" + registryName + ".azurecr.io/v2/" + repositoryName + "/tags/list")
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore lista tag ACR: " + e.getMessage()
                        + " (nota: richiede autenticazione specifica ACR)")));
    }
}
