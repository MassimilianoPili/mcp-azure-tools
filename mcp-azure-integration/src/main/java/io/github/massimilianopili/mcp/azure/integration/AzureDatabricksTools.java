package io.github.massimilianopili.mcp.azure.integration;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureDatabricksTools {

    private static final String API = "2024-05-01";
    private static final String P   = "Microsoft.Databricks/workspaces";

    private final WebClient w;
    private final AzureProperties props;

    public AzureDatabricksTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_databricks_workspaces",
          description = "Elenca tutti i workspace Azure Databricks nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listDatabricksWorkspaces() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(ws -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ws.getOrDefault("name", ""));
                        r.put("location", ws.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) ws.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) ws.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("workspaceUrl", p.getOrDefault("workspaceUrl", ""));
                        r.put("workspaceId", p.getOrDefault("workspaceId", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_databricks_workspace",
          description = "Recupera i dettagli di un workspace Azure Databricks")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getDatabricksWorkspace(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del workspace Databricks") String workspaceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + workspaceName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_databricks_workspace",
          description = "Elimina un workspace Azure Databricks")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteDatabricksWorkspace(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del workspace Databricks da eliminare") String workspaceName) {
        return w.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + workspaceName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminazione avviata per " + workspaceName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
