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
public class AzureSqlTools {

    private static final String API_VERSION = "2023-08-01";
    private static final String PROVIDER    = "/providers/Microsoft.Sql/servers";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureSqlTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_sql_servers",
          description = "Elenca tutti i server Azure SQL nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSqlServers() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> servers = (List<Map<String, Object>>) response.get("value");
                    return servers.stream().map(s -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", s.getOrDefault("name", ""));
                        r.put("location", s.getOrDefault("location", ""));
                        r.put("fullyQualifiedDomainName", s.containsKey("properties")
                                ? ((Map<String, Object>) s.get("properties")).getOrDefault("fullyQualifiedDomainName", "")
                                : "");
                        r.put("state", s.containsKey("properties")
                                ? ((Map<String, Object>) s.get("properties")).getOrDefault("state", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista SQL server: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_sql_databases",
          description = "Elenca tutti i database di un server Azure SQL")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSqlDatabases(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del SQL server") String serverName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serverName
                        + "/databases?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> dbs = (List<Map<String, Object>>) response.get("value");
                    return dbs.stream().map(db -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", db.getOrDefault("name", ""));
                        r.put("location", db.getOrDefault("location", ""));
                        r.put("sku", db.containsKey("sku") ? ((Map<String, Object>) db.get("sku")).getOrDefault("name", "") : "");
                        r.put("status", db.containsKey("properties")
                                ? ((Map<String, Object>) db.get("properties")).getOrDefault("status", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista database SQL: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_sql_database",
          description = "Recupera i dettagli di un database Azure SQL")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getSqlDatabase(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del SQL server") String serverName,
            @ToolParam(description = "Nome del database") String dbName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serverName
                        + "/databases/" + dbName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero database SQL: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_sql_database",
          description = "Crea un nuovo database su un server Azure SQL")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createSqlDatabase(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del SQL server") String serverName,
            @ToolParam(description = "Nome del nuovo database") String dbName,
            @ToolParam(description = "Location Azure, es: italynorth, westeurope") String location,
            @ToolParam(description = "SKU: Basic, Standard, Premium, GeneralPurpose (default: Basic)", required = false) String sku) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("location", location);
        body.put("sku", Map.of("name", (sku != null && !sku.isBlank()) ? sku : "Basic"));
        body.put("properties", Map.of("collation", "SQL_Latin1_General_CP1_CI_AS"));

        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serverName
                        + "/databases/" + dbName + "?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione database SQL: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_sql_firewall_rules",
          description = "Elenca le regole firewall di un server Azure SQL")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSqlFirewallRules(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del SQL server") String serverName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serverName
                        + "/firewallRules?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> rules = (List<Map<String, Object>>) response.get("value");
                    return rules.stream().map(rule -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", rule.getOrDefault("name", ""));
                        r.put("startIpAddress", rule.containsKey("properties")
                                ? ((Map<String, Object>) rule.get("properties")).getOrDefault("startIpAddress", "")
                                : "");
                        r.put("endIpAddress", rule.containsKey("properties")
                                ? ((Map<String, Object>) rule.get("properties")).getOrDefault("endIpAddress", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista regole firewall SQL: " + e.getMessage()))));
    }
}
