package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureMySqlTools {

    private static final String API_VERSION = "2023-12-30";
    private static final String PROVIDER    = "/providers/Microsoft.DBforMySQL/flexibleServers";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureMySqlTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_mysql_servers",
          description = "Elenca tutti i server MySQL Flexible nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listMySqlServers() {
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
                        Map<String, Object> sku = (Map<String, Object>) s.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) s.getOrDefault("properties", Map.of());
                        r.put("version", p.getOrDefault("version", ""));
                        r.put("state", p.getOrDefault("state", ""));
                        r.put("fullyQualifiedDomainName", p.getOrDefault("fullyQualifiedDomainName", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista MySQL servers: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_mysql_server",
          description = "Recupera i dettagli di un server MySQL Flexible Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getMySqlServer(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del server MySQL") String serverName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + serverName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero MySQL server: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_mysql_databases",
          description = "Elenca i database in un server MySQL Flexible Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listMySqlDatabases(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del server MySQL") String serverName) {
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
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista database MySQL: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_create_mysql_server",
          description = "Crea un nuovo server MySQL Flexible Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createMySqlServer(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del server (univoco globalmente)") String serverName,
            @ToolParam(description = "Regione Azure, es: westeurope") String location,
            @ToolParam(description = "SKU compute, es: Standard_D2ds_v4") String skuName,
            @ToolParam(description = "Username amministratore") String adminUser,
            @ToolParam(description = "Password amministratore") String adminPassword,
            @ToolParam(description = "Versione MySQL, es: 8.0.21") String version) {
        Map<String, Object> body = Map.of(
                "location", location,
                "sku", Map.of("name", skuName, "tier", "GeneralPurpose"),
                "properties", Map.of(
                        "administratorLogin", adminUser,
                        "administratorLoginPassword", adminPassword,
                        "version", version,
                        "storage", Map.of("storageSizeGB", 20, "autoGrow", "Enabled"),
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
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione MySQL server: " + e.getMessage())));
    }
}
