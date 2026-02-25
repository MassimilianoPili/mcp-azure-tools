package io.github.massimilianopili.mcp.azure.data;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureCosmosDbTools {

    private static final String API_VERSION = "2024-05-15";
    private static final String PROVIDER    = "/providers/Microsoft.DocumentDB/databaseAccounts";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureCosmosDbTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_cosmosdb_accounts",
          description = "Elenca tutti gli account Azure Cosmos DB nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listCosmosDbAccounts() {
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
                        r.put("documentEndpoint", a.containsKey("properties")
                                ? ((Map<String, Object>) a.get("properties")).getOrDefault("documentEndpoint", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista CosmosDB: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_cosmosdb_account",
          description = "Recupera i dettagli di un account Azure Cosmos DB")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getCosmosDbAccount(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'account CosmosDB") String accountName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + accountName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero account CosmosDB: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_cosmosdb_databases",
          description = "Elenca i database SQL di un account Azure Cosmos DB")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listCosmosDbDatabases(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'account CosmosDB") String accountName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + accountName
                        + "/sqlDatabases?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> dbs = (List<Map<String, Object>>) response.get("value");
                    return dbs.stream().map(db -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", db.getOrDefault("name", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista database CosmosDB: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_cosmosdb_containers",
          description = "Elenca i container SQL di un database Azure Cosmos DB")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listCosmosDbContainers(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'account CosmosDB") String accountName,
            @ToolParam(description = "Nome del database CosmosDB") String dbName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + accountName
                        + "/sqlDatabases/" + dbName + "/containers?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> containers = (List<Map<String, Object>>) response.get("value");
                    return containers.stream().map(c -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", c.getOrDefault("name", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista container CosmosDB: " + e.getMessage()))));
    }
}
