package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzurePostgresTools {

    private static final String API_VERSION = "2023-06-01-preview";
    private static final String PROVIDER    = "/providers/Microsoft.DBforPostgreSQL/flexibleServers";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzurePostgresTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_postgres_servers",
          description = "Elenca tutti i server PostgreSQL Flexible nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPostgresServers() {
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
                        r.put("resourceGroup", extractResourceGroup((String) s.getOrDefault("id", "")));
                        Map<String, Object> sku = (Map<String, Object>) s.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) s.getOrDefault("properties", Map.of());
                        r.put("version", p.getOrDefault("version", ""));
                        r.put("state", p.getOrDefault("state", ""));
                        r.put("fullyQualifiedDomainName", p.getOrDefault("fullyQualifiedDomainName", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista PostgreSQL servers: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_postgres_server",
          description = "Recupera i dettagli di un server PostgreSQL Flexible Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getPostgresServer(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del server PostgreSQL") String serverName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serverName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero PostgreSQL server: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_postgres_databases",
          description = "Elenca i database in un server PostgreSQL Flexible Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPostgresDatabases(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del server PostgreSQL") String serverName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serverName + "/databases?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> dbs = (List<Map<String, Object>>) response.get("value");
                    return dbs.stream().map(db -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", db.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) db.getOrDefault("properties", Map.of());
                        r.put("charset", p.getOrDefault("charset", ""));
                        r.put("collation", p.getOrDefault("collation", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista database PostgreSQL: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_create_postgres_server",
          description = "Crea un nuovo server PostgreSQL Flexible Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createPostgresServer(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del server (univoco globalmente)") String serverName,
            @ToolParam(description = "Regione Azure, es: westeurope") String location,
            @ToolParam(description = "SKU compute, es: Standard_D2s_v3") String skuName,
            @ToolParam(description = "Username amministratore") String adminUser,
            @ToolParam(description = "Password amministratore") String adminPassword,
            @ToolParam(description = "Versione PostgreSQL, es: 16") String version) {
        Map<String, Object> body = Map.of(
                "location", location,
                "sku", Map.of("name", skuName, "tier", "GeneralPurpose"),
                "properties", Map.of(
                        "administratorLogin", adminUser,
                        "administratorLoginPassword", adminPassword,
                        "version", version,
                        "storage", Map.of("storageSizeGB", 32),
                        "backup", Map.of("backupRetentionDays", 7, "geoRedundantBackup", "Disabled"),
                        "highAvailability", Map.of("mode", "Disabled")
                )
        );
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serverName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione PostgreSQL server: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_postgres_configurations",
          description = "Elenca i parametri di configurazione di un server PostgreSQL Flexible Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPostgresConfigurations(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del server PostgreSQL") String serverName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serverName + "/configurations?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> configs = (List<Map<String, Object>>) response.get("value");
                    return configs.stream().map(c -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", c.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) c.getOrDefault("properties", Map.of());
                        r.put("value", p.getOrDefault("value", ""));
                        r.put("defaultValue", p.getOrDefault("defaultValue", ""));
                        r.put("description", p.getOrDefault("description", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista configurazioni PostgreSQL: " + e.getMessage()))));
    }

    private String extractResourceGroup(String id) {
        String[] parts = id.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("resourceGroups".equalsIgnoreCase(parts[i])) return parts[i + 1];
        }
        return "";
    }
}
