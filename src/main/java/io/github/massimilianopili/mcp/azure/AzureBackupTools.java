package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureBackupTools {

    private static final String API_VERSION = "2023-08-01";
    private static final String PROVIDER    = "/providers/Microsoft.RecoveryServices/vaults";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureBackupTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_recovery_vaults",
          description = "Elenca tutti i Recovery Services Vault nella subscription Azure (backup di VM, PostgreSQL, etc.)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listRecoveryVaults() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> vaults = (List<Map<String, Object>>) response.get("value");
                    return vaults.stream().map(v -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", v.getOrDefault("name", ""));
                        r.put("location", v.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) v.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) v.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista recovery vaults: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_recovery_vault",
          description = "Recupera i dettagli di un Recovery Services Vault Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getRecoveryVault(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del Recovery Services Vault") String vaultName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + vaultName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero recovery vault: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_recovery_vault",
          description = "Crea un Recovery Services Vault Azure per backup di VM e database")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createRecoveryVault(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del vault") String vaultName,
            @ToolParam(description = "Regione Azure, es: westeurope") String location) {
        Map<String, Object> body = Map.of(
                "location", location,
                "sku", Map.of("name", "Standard", "tier", "Standard"),
                "properties", Map.of()
        );
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + vaultName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione recovery vault: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_backup_protected_items",
          description = "Elenca gli item protetti da backup in un Recovery Services Vault Azure (VM, DB)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listBackupProtectedItems(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del Recovery Services Vault") String vaultName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + vaultName
                        + "/backupProtectedItems?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
                    return items.stream().map(item -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", item.getOrDefault("name", ""));
                        r.put("type", item.getOrDefault("type", ""));
                        Map<String, Object> p = (Map<String, Object>) item.getOrDefault("properties", Map.of());
                        r.put("friendlyName", p.getOrDefault("friendlyName", ""));
                        r.put("protectionStatus", p.getOrDefault("protectionStatus", ""));
                        r.put("lastBackupStatus", p.getOrDefault("lastBackupStatus", ""));
                        r.put("lastBackupTime", p.getOrDefault("lastBackupTime", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista backup protected items: " + e.getMessage()))));
    }
}
