package io.github.massimilianopili.mcp.azure.security;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureManagedIdentityTools {

    private static final String API_VERSION = "2023-01-31";
    private static final String PROVIDER    = "/providers/Microsoft.ManagedIdentity/userAssignedIdentities";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureManagedIdentityTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_user_assigned_identities",
          description = "Elenca tutte le User-Assigned Managed Identity nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listUserAssignedIdentities() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> ids = (List<Map<String, Object>>) response.get("value");
                    return ids.stream().map(id -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", id.getOrDefault("name", ""));
                        r.put("location", id.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) id.getOrDefault("properties", Map.of());
                        r.put("clientId", p.getOrDefault("clientId", ""));
                        r.put("principalId", p.getOrDefault("principalId", ""));
                        r.put("tenantId", p.getOrDefault("tenantId", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista managed identities: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_user_assigned_identity",
          description = "Recupera i dettagli di una User-Assigned Managed Identity Azure (clientId, principalId)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getUserAssignedIdentity(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della managed identity") String identityName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + identityName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero managed identity: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_user_assigned_identity",
          description = "Crea una User-Assigned Managed Identity Azure (service account senza credenziali)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createUserAssignedIdentity(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della managed identity") String identityName,
            @ToolParam(description = "Regione Azure, es: westeurope") String location) {
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + identityName + "?api-version=" + API_VERSION)
                .bodyValue(Map.of("location", location))
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione managed identity: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_user_assigned_identity",
          description = "Elimina una User-Assigned Managed Identity Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteUserAssignedIdentity(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della managed identity") String identityName) {
        return webClient.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + identityName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminata: " + identityName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione managed identity: " + e.getMessage())));
    }
}
