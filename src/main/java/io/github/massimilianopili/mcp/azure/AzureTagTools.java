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
public class AzureTagTools {

    private static final String API_VERSION = "2021-04-01";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureTagTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_available_tags",
          description = "Elenca tutti i tag name disponibili nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAvailableTags() {
        return webClient.get()
                .uri(props.getArmBase() + "/tagNames?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> tags = (List<Map<String, Object>>) response.get("value");
                    return tags.stream().map(t -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("tagName", t.getOrDefault("tagName", ""));
                        r.put("count", t.containsKey("count")
                                ? ((Map<String, Object>) t.get("count")).getOrDefault("value", 0)
                                : 0);
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista tag disponibili: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_resources_by_tag",
          description = "Elenca le risorse Azure filtrate per tag key e value")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listResourcesByTag(
            @ToolParam(description = "Chiave del tag, es: environment") String tagKey,
            @ToolParam(description = "Valore del tag, es: production") String tagValue) {
        String filter = "tagName eq '" + tagKey + "' and tagValue eq '" + tagValue + "'";
        return webClient.get()
                .uri(props.getArmBase() + "/resources?$filter=" + filter + "&api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("value");
                    return resources.stream().map(res -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", res.getOrDefault("name", ""));
                        r.put("type", res.getOrDefault("type", ""));
                        r.put("location", res.getOrDefault("location", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista risorse per tag: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_update_resource_tags",
          description = "Aggiorna i tag di una risorsa Azure specificando il resource ID completo")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> updateResourceTags(
            @ToolParam(description = "Resource ID completo, es: /subscriptions/{sub}/resourceGroups/{rg}/providers/Microsoft.Compute/virtualMachines/{name}") String resourceId,
            @ToolParam(description = "Tag in formato chiave=valore separati da virgola, es: env=prod,team=backend") String tags) {
        Map<String, String> tagsMap = new LinkedHashMap<>();
        for (String pair : tags.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) tagsMap.put(kv[0].trim(), kv[1].trim());
        }

        return webClient.patch()
                .uri("https://management.azure.com" + resourceId + "?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("tags", tagsMap))
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore aggiornamento tag risorsa: " + e.getMessage())));
    }
}
