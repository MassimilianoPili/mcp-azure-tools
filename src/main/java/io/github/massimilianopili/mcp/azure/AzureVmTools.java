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
public class AzureVmTools {

    private static final String API_VERSION = "2024-07-01";
    private static final String PROVIDER = "/providers/Microsoft.Compute/virtualMachines";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureVmTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_vms",
          description = "Elenca tutte le virtual machine nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listVms() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> vms = (List<Map<String, Object>>) response.get("value");
                    return vms.stream().map(vm -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", vm.getOrDefault("name", ""));
                        r.put("location", vm.getOrDefault("location", ""));
                        r.put("resourceGroup", extractRgFromId((String) vm.getOrDefault("id", "")));
                        r.put("vmSize", vm.containsKey("properties")
                                ? ((Map<String, Object>) ((Map<String, Object>) vm.get("properties")).getOrDefault("hardwareProfile", Map.of())).getOrDefault("vmSize", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista VM: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_vm",
          description = "Recupera i dettagli di una virtual machine Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getVm(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della VM") String vmName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + vmName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero VM: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_start_vm",
          description = "Avvia una virtual machine Azure")
    public Mono<Map<String, Object>> startVm(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della VM") String vmName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + vmName + "/start?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "message", "Avvio VM " + vmName + " avviato"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore avvio VM: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_stop_vm",
          description = "Arresta e dealloca una virtual machine Azure (billing fermato)")
    public Mono<Map<String, Object>> stopVm(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della VM") String vmName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + vmName + "/deallocate?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "message", "Arresto VM " + vmName + " avviato"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore arresto VM: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_restart_vm",
          description = "Riavvia una virtual machine Azure")
    public Mono<Map<String, Object>> restartVm(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della VM") String vmName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + vmName + "/restart?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "message", "Riavvio VM " + vmName + " avviato"))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore riavvio VM: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_get_vm_status",
          description = "Recupera lo stato runtime di una virtual machine Azure (powerState)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getVmStatus(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della VM") String vmName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + vmName + "/instanceView?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("vmName", vmName);
                    List<Map<String, Object>> statuses = (List<Map<String, Object>>) response.getOrDefault("statuses", List.of());
                    statuses.stream()
                            .filter(s -> ((String) s.getOrDefault("code", "")).startsWith("PowerState/"))
                            .findFirst()
                            .ifPresent(s -> r.put("powerState", s.getOrDefault("displayStatus", "")));
                    return r;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore stato VM: " + e.getMessage())));
    }

    private String extractRgFromId(String id) {
        if (id == null || id.isEmpty()) return "";
        String[] parts = id.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("resourceGroups".equalsIgnoreCase(parts[i])) return parts[i + 1];
        }
        return "";
    }
}
