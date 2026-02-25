package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureVmssTools {

    private static final String API = "2024-07-01";
    private static final String P   = "Microsoft.Compute/virtualMachineScaleSets";

    private final WebClient w;
    private final AzureProperties props;

    public AzureVmssTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_vmss",
          description = "Elenca tutti i Virtual Machine Scale Set nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listVmss() {
        return w.get()
                .uri(props.getArmBase() + "/providers/" + P + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(vmss -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", vmss.getOrDefault("name", ""));
                        r.put("location", vmss.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) vmss.getOrDefault("sku", Map.of());
                        r.put("vmSize", sku.getOrDefault("name", ""));
                        r.put("capacity", sku.getOrDefault("capacity", 0));
                        Map<String, Object> p = (Map<String, Object>) vmss.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("orchestrationMode", p.getOrDefault("orchestrationMode", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_vmss",
          description = "Recupera i dettagli di un Virtual Machine Scale Set")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getVmss(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del VMSS") String vmssName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + vmssName + "?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_vmss_instances",
          description = "Elenca le istanze VM di un Virtual Machine Scale Set")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listVmssInstances(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del VMSS") String vmssName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + vmssName + "/virtualMachines?api-version=" + API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(vm -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", vm.getOrDefault("name", ""));
                        r.put("instanceId", vm.getOrDefault("instanceId", ""));
                        Map<String, Object> p = (Map<String, Object>) vm.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("latestModelApplied", p.getOrDefault("latestModelApplied", false));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_scale_vmss",
          description = "Scala un VMSS modificando la capacity (numero di istanze)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> scaleVmss(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del VMSS") String vmssName,
            @ToolParam(description = "Numero desiderato di istanze") int capacity) {
        Map<String, Object> body = Map.of("sku", Map.of("capacity", capacity));
        return w.patch()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/" + P + "/" + vmssName + "?api-version=" + API)
                .bodyValue(body)
                .retrieve().bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }
}
