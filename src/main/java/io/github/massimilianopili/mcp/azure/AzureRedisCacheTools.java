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
public class AzureRedisCacheTools {

    private static final String API_VERSION = "2024-03-01";
    private static final String PROVIDER    = "/providers/Microsoft.Cache/redis";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureRedisCacheTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_redis_caches",
          description = "Elenca tutte le istanze Azure Cache for Redis nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listRedisCaches() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> caches = (List<Map<String, Object>>) response.get("value");
                    return caches.stream().map(c -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", c.getOrDefault("name", ""));
                        r.put("location", c.getOrDefault("location", ""));
                        r.put("sku", c.containsKey("properties")
                                ? ((Map<String, Object>) c.get("properties")).getOrDefault("sku", Map.of())
                                : Map.of());
                        r.put("hostName", c.containsKey("properties")
                                ? ((Map<String, Object>) c.get("properties")).getOrDefault("hostName", "")
                                : "");
                        r.put("provisioningState", c.containsKey("properties")
                                ? ((Map<String, Object>) c.get("properties")).getOrDefault("provisioningState", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Redis Cache: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_redis_cache",
          description = "Recupera i dettagli di un'istanza Azure Cache for Redis")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getRedisCache(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'istanza Redis") String cacheName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + cacheName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Redis Cache: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_get_redis_access_keys",
          description = "Recupera le chiavi di accesso (primary/secondary) di un'istanza Azure Cache for Redis")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getRedisAccessKeys(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'istanza Redis") String cacheName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + cacheName
                        + "/listKeys?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero chiavi Redis: " + e.getMessage())));
    }
}
