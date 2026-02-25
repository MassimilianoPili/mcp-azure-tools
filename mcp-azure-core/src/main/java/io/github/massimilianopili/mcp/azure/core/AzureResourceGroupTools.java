package io.github.massimilianopili.mcp.azure.core;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureResourceGroupTools {

    private static final String API_VERSION = "2021-04-01";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureResourceGroupTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_resource_groups",
          description = "Elenca tutti i resource group nella subscription Azure corrente")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listResourceGroups() {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> rgs = (List<Map<String, Object>>) response.get("value");
                    return rgs.stream().map(rg -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", rg.getOrDefault("name", ""));
                        r.put("location", rg.getOrDefault("location", ""));
                        r.put("provisioningState", rg.containsKey("properties")
                                ? ((Map<String, Object>) rg.get("properties")).getOrDefault("provisioningState", "")
                                : "");
                        r.put("tags", rg.getOrDefault("tags", Map.of()));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista resource group: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_resource_group",
          description = "Recupera i dettagli di un resource group Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getResourceGroup(
            @ToolParam(description = "Nome del resource group") String rgName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + rgName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero resource group: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_resource_group",
          description = "Crea un nuovo resource group nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createResourceGroup(
            @ToolParam(description = "Nome del resource group") String name,
            @ToolParam(description = "Location Azure, es: italynorth, westeurope, eastus") String location,
            @ToolParam(description = "Tag in formato chiave=valore separati da virgola, es: env=prod,team=backend", required = false) String tags) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("location", location);
        if (tags != null && !tags.isBlank()) {
            Map<String, String> tagsMap = new LinkedHashMap<>();
            for (String pair : tags.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) tagsMap.put(kv[0].trim(), kv[1].trim());
            }
            body.put("tags", tagsMap);
        }

        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + name + "?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione resource group: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_resource_group",
          description = "Elimina un resource group Azure e tutte le risorse al suo interno. Operazione asincrona.")
    public Mono<Map<String, Object>> deleteResourceGroup(
            @ToolParam(description = "Nome del resource group da eliminare") String rgName) {
        return webClient.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + rgName + "?api-version=" + API_VERSION)
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "message", "Eliminazione avviata per: " + rgName))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione resource group: " + e.getMessage())));
    }
}
