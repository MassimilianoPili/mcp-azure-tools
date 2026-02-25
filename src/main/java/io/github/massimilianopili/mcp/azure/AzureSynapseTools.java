package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureSynapseTools {

    private static final String API = "2021-06-01";
    private static final String P   = "Microsoft.Synapse/workspaces";

    private final WebClient w;
    private final AzureProperties props;

    public AzureSynapseTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_synapse_workspaces",
          description = "Elenca tutti i workspace Azure Synapse Analytics nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSynapseWorkspaces() {
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
                        Map<String, Object> p = (Map<String, Object>) ws.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("connectivityEndpoints", p.getOrDefault("connectivityEndpoints", Map.of()));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_synapse_workspace",
          description = "Recupera i dettagli di un workspace Azure Synapse Analytics")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getSynapseWorkspace(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del workspace Synapse") String workspaceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + workspaceName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_synapse_sql_pools",
          description = "Elenca i SQL pool dedicati di un workspace Synapse")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSynapseSqlPools(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del workspace Synapse") String workspaceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + workspaceName + "/sqlPools?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(pool -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", pool.getOrDefault("name", ""));
                        Map<String, Object> sku = (Map<String, Object>) pool.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) pool.getOrDefault("properties", Map.of());
                        r.put("status", p.getOrDefault("status", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_synapse_spark_pools",
          description = "Elenca gli Apache Spark pool di un workspace Synapse")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSynapseSparkPools(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del workspace Synapse") String workspaceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + workspaceName + "/bigDataPools?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(pool -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", pool.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) pool.getOrDefault("properties", Map.of());
                        r.put("nodeSize", p.getOrDefault("nodeSize", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("sparkVersion", p.getOrDefault("sparkVersion", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
