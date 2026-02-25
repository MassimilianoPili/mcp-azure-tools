package io.github.massimilianopili.mcp.azure.compute;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureContainerInstanceTools {

    private static final String API_VERSION = "2023-05-01";
    private static final String PROVIDER    = "/providers/Microsoft.ContainerInstance/containerGroups";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureContainerInstanceTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_container_groups",
          description = "Elenca tutti i container group Azure Container Instances (ACI) nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listContainerGroups() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> groups = (List<Map<String, Object>>) response.get("value");
                    return groups.stream().map(g -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", g.getOrDefault("name", ""));
                        r.put("location", g.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) g.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        Object iv = p.getOrDefault("instanceView", Map.of());
                        r.put("state", iv instanceof Map ? ((Map<String, Object>) iv).getOrDefault("state", "") : "");
                        r.put("osType", p.getOrDefault("osType", ""));
                        r.put("restartPolicy", p.getOrDefault("restartPolicy", ""));
                        List<Map<String, Object>> containers = (List<Map<String, Object>>) p.getOrDefault("containers", List.of());
                        r.put("containers", containers.stream().map(c -> c.getOrDefault("name", "")).toList());
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista container groups ACI: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_container_group",
          description = "Recupera i dettagli di un container group Azure Container Instances (stato, IP, log)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getContainerGroup(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del container group") String groupName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + groupName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero container group ACI: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_create_container_group",
          description = "Crea un container group Azure Container Instances con un singolo container")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createContainerGroup(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del container group") String groupName,
            @ToolParam(description = "Regione Azure, es: westeurope") String location,
            @ToolParam(description = "Nome del container") String containerName,
            @ToolParam(description = "Immagine Docker, es: nginx:latest o myregistry.azurecr.io/myapp:1.0") String image,
            @ToolParam(description = "CPU cores (es: 1.0)") double cpu,
            @ToolParam(description = "Memoria in GB (es: 1.5)") double memoryGb,
            @ToolParam(description = "Porta da esporre (0 = nessuna porta pubblica)") int port) {
        List<Map<String, Object>> ports = port > 0
                ? List.of(Map.of("port", port, "protocol", "TCP"))
                : List.of();
        Map<String, Object> container = new LinkedHashMap<>();
        container.put("name", containerName);
        container.put("properties", Map.of(
                "image", image,
                "resources", Map.of("requests", Map.of("cpu", cpu, "memoryInGB", memoryGb)),
                "ports", ports
        ));
        Map<String, Object> ipAddress = port > 0
                ? Map.of("type", "Public", "ports", ports)
                : Map.of("type", "None");
        Map<String, Object> body = Map.of(
                "location", location,
                "properties", Map.of(
                        "containers", List.of(container),
                        "osType", "Linux",
                        "restartPolicy", "Always",
                        "ipAddress", ipAddress
                )
        );
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + groupName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione container group ACI: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_start_container_group",
          description = "Avvia (o riavvia) un container group Azure Container Instances")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> startContainerGroup(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del container group") String groupName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + groupName + "/start?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Start richiesto per " + groupName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore avvio container group ACI: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_container_group",
          description = "Elimina un container group Azure Container Instances")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> deleteContainerGroup(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del container group da eliminare") String groupName) {
        return webClient.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + groupName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Eliminazione avviata per " + groupName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione container group ACI: " + e.getMessage())));
    }
}
