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
public class AzureKeyVaultTools {

    private static final String ARM_API_VERSION = "2023-07-01";
    private static final String KV_API_VERSION  = "7.4";
    private static final String PROVIDER        = "/providers/Microsoft.KeyVault/vaults";

    private final WebClient armWebClient;
    private final WebClient kvWebClient;
    private final AzureProperties props;

    public AzureKeyVaultTools(
            @Qualifier("azureArmWebClient") WebClient armWebClient,
            @Qualifier("azureKvWebClient") WebClient kvWebClient,
            AzureProperties props) {
        this.armWebClient = armWebClient;
        this.kvWebClient = kvWebClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_key_vaults",
          description = "Elenca tutti i Key Vault nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listKeyVaults() {
        return armWebClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + ARM_API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> vaults = (List<Map<String, Object>>) response.get("value");
                    return vaults.stream().map(v -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", v.getOrDefault("name", ""));
                        r.put("location", v.getOrDefault("location", ""));
                        r.put("vaultUri", v.containsKey("properties")
                                ? ((Map<String, Object>) v.get("properties")).getOrDefault("vaultUri", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Key Vault: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_secrets",
          description = "Elenca i nomi dei segreti in un Key Vault Azure (non i valori)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSecrets(
            @ToolParam(description = "Nome del Key Vault, es: mio-keyvault") String vaultName) {
        return kvWebClient.get()
                .uri("https://" + vaultName + ".vault.azure.net/secrets?api-version=" + KV_API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> secrets = (List<Map<String, Object>>) response.get("value");
                    return secrets.stream().map(s -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        String id = (String) s.getOrDefault("id", "");
                        r.put("name", id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id);
                        r.put("enabled", s.containsKey("attributes")
                                ? ((Map<String, Object>) s.get("attributes")).getOrDefault("enabled", true)
                                : true);
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista segreti: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_secret",
          description = "Recupera il valore di un segreto da un Key Vault Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getSecret(
            @ToolParam(description = "Nome del Key Vault") String vaultName,
            @ToolParam(description = "Nome del segreto") String secretName) {
        return kvWebClient.get()
                .uri("https://" + vaultName + ".vault.azure.net/secrets/" + secretName + "?api-version=" + KV_API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero segreto: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_set_secret",
          description = "Crea o aggiorna un segreto in un Key Vault Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> setSecret(
            @ToolParam(description = "Nome del Key Vault") String vaultName,
            @ToolParam(description = "Nome del segreto") String secretName,
            @ToolParam(description = "Valore del segreto") String value) {
        return kvWebClient.put()
                .uri("https://" + vaultName + ".vault.azure.net/secrets/" + secretName + "?api-version=" + KV_API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("value", value))
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore impostazione segreto: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_secret",
          description = "Elimina un segreto da un Key Vault Azure (soft delete)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteSecret(
            @ToolParam(description = "Nome del Key Vault") String vaultName,
            @ToolParam(description = "Nome del segreto da eliminare") String secretName) {
        return kvWebClient.delete()
                .uri("https://" + vaultName + ".vault.azure.net/secrets/" + secretName + "?api-version=" + KV_API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione segreto: " + e.getMessage())));
    }
}
