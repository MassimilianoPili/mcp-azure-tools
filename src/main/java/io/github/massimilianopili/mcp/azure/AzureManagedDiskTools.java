package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureManagedDiskTools {

    private static final String API = "2024-03-02";
    private static final String P   = "Microsoft.Compute/disks";

    private final WebClient w;
    private final AzureProperties props;

    public AzureManagedDiskTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_managed_disks",
          description = "Elenca tutti i managed disk Azure nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listManagedDisks() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(d -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", d.getOrDefault("name", ""));
                        r.put("location", d.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) d.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) d.getOrDefault("properties", Map.of());
                        r.put("diskSizeGB", p.getOrDefault("diskSizeGB", 0));
                        r.put("diskState", p.getOrDefault("diskState", ""));
                        r.put("osType", p.getOrDefault("osType", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_managed_disk",
          description = "Recupera i dettagli di un managed disk Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getManagedDisk(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del disco") String diskName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + diskName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_managed_disk",
          description = "Elimina un managed disk Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteManagedDisk(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del disco da eliminare") String diskName) {
        return w.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + diskName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminazione avviata per " + diskName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
