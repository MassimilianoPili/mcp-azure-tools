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
public class AzureMLTools {

    private static final String API = "2024-10-01";
    private static final String P   = "Microsoft.MachineLearningServices/workspaces";

    private final WebClient w;
    private final AzureProperties props;

    public AzureMLTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_ml_workspaces",
          description = "Elenca tutti i workspace Azure Machine Learning nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listMlWorkspaces() {
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
                        r.put("workspaceId", p.getOrDefault("workspaceId", ""));
                        r.put("mlFlowTrackingUri", p.getOrDefault("mlFlowTrackingUri", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_ml_workspace",
          description = "Recupera i dettagli di un workspace Azure Machine Learning")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getMlWorkspace(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del workspace ML") String workspaceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + workspaceName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_ml_compute_targets",
          description = "Elenca i compute target di un workspace Azure ML")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listMlComputeTargets(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del workspace ML") String workspaceName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + workspaceName + "/computes?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(c -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", c.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) c.getOrDefault("properties", Map.of());
                        r.put("computeType", p.getOrDefault("computeType", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("description", p.getOrDefault("description", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
