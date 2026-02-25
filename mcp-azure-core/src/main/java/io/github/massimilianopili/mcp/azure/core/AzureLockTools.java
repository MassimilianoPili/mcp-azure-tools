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
public class AzureLockTools {

    private static final String API_VERSION = "2020-05-01";
    private static final String LOCKS_PATH  = "/providers/Microsoft.Authorization/locks";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureLockTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_locks",
          description = "Elenca tutti i management lock nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listLocks() {
        return webClient.get()
                .uri(props.getArmBase() + LOCKS_PATH + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> locks = (List<Map<String, Object>>) response.get("value");
                    return locks.stream().map(l -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", l.getOrDefault("name", ""));
                        r.put("level", l.containsKey("properties")
                                ? ((Map<String, Object>) l.get("properties")).getOrDefault("level", "")
                                : "");
                        r.put("notes", l.containsKey("properties")
                                ? ((Map<String, Object>) l.get("properties")).getOrDefault("notes", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista lock: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_create_lock",
          description = "Crea un management lock sulla subscription Azure. Livelli: CanNotDelete (blocca eliminazione), ReadOnly (blocca modifiche)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createLock(
            @ToolParam(description = "Nome del lock") String lockName,
            @ToolParam(description = "Livello: CanNotDelete o ReadOnly") String level,
            @ToolParam(description = "Note esplicative sul lock", required = false) String notes) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("level", level);
        if (notes != null && !notes.isBlank()) properties.put("notes", notes);

        return webClient.put()
                .uri(props.getArmBase() + LOCKS_PATH + "/" + lockName + "?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("properties", properties))
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione lock: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_lock",
          description = "Elimina un management lock dalla subscription Azure")
    public Mono<Map<String, Object>> deleteLock(
            @ToolParam(description = "Nome del lock da eliminare") String lockName) {
        return webClient.delete()
                .uri(props.getArmBase() + LOCKS_PATH + "/" + lockName + "?api-version=" + API_VERSION)
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "deleted", true, "lockName", lockName))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione lock: " + e.getMessage())));
    }
}
