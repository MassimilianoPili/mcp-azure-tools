package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureDataFactoryTools {

    private static final String API = "2018-06-01";
    private static final String P   = "Microsoft.DataFactory/factories";

    private final WebClient w;
    private final AzureProperties props;

    public AzureDataFactoryTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_data_factories",
          description = "Elenca tutte le Azure Data Factory nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listDataFactories() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(f -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", f.getOrDefault("name", ""));
                        r.put("location", f.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) f.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("createTime", p.getOrDefault("createTime", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_data_factory",
          description = "Recupera i dettagli di una Azure Data Factory")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getDataFactory(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Data Factory") String factoryName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + factoryName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_adf_pipelines",
          description = "Elenca le pipeline di una Azure Data Factory")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAdfPipelines(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Data Factory") String factoryName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + factoryName + "/pipelines?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(pl -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", pl.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) pl.getOrDefault("properties", Map.of());
                        r.put("description", p.getOrDefault("description", ""));
                        List<?> acts = (List<?>) p.getOrDefault("activities", List.of());
                        r.put("activityCount", acts.size());
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_adf_datasets",
          description = "Elenca i dataset di una Azure Data Factory")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAdfDatasets(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della Data Factory") String factoryName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + factoryName + "/datasets?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(ds -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ds.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) ds.getOrDefault("properties", Map.of());
                        r.put("type", p.getOrDefault("type", ""));
                        r.put("description", p.getOrDefault("description", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
