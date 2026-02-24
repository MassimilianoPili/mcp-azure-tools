package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureStorageTools {

    private static final String API_VERSION = "2023-05-01";
    private static final String PROVIDER = "/providers/Microsoft.Storage/storageAccounts";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureStorageTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_storage_accounts",
          description = "Elenca tutti gli storage account nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listStorageAccounts() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> accounts = (List<Map<String, Object>>) response.get("value");
                    return accounts.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", a.getOrDefault("name", ""));
                        r.put("location", a.getOrDefault("location", ""));
                        r.put("kind", a.getOrDefault("kind", ""));
                        r.put("sku", a.containsKey("sku") ? ((Map<String, Object>) a.get("sku")).getOrDefault("name", "") : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista storage account: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_storage_account",
          description = "Recupera i dettagli di uno storage account Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getStorageAccount(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dello storage account") String accountName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + accountName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero storage account: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_storage_containers",
          description = "Elenca i blob container di uno storage account Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listStorageContainers(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dello storage account") String accountName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/"
                        + accountName + "/blobServices/default/containers?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> containers = (List<Map<String, Object>>) response.get("value");
                    return containers.stream().map(c -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", c.getOrDefault("name", ""));
                        r.put("publicAccess", c.containsKey("properties")
                                ? ((Map<String, Object>) c.get("properties")).getOrDefault("publicAccess", "None")
                                : "None");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista container: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_create_storage_account",
          description = "Crea un nuovo storage account Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createStorageAccount(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dello storage account (3-24 caratteri, solo minuscole e numeri)") String accountName,
            @ToolParam(description = "Location Azure, es: italynorth, westeurope") String location,
            @ToolParam(description = "SKU: Standard_LRS, Standard_GRS, Standard_RAGRS, Premium_LRS (default: Standard_LRS)", required = false) String sku,
            @ToolParam(description = "Kind: StorageV2, BlobStorage, FileStorage (default: StorageV2)", required = false) String kind) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sku", Map.of("name", (sku != null && !sku.isBlank()) ? sku : "Standard_LRS"));
        body.put("kind", (kind != null && !kind.isBlank()) ? kind : "StorageV2");
        body.put("location", location);

        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + accountName + "?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione storage account: " + e.getMessage())));
    }
}
