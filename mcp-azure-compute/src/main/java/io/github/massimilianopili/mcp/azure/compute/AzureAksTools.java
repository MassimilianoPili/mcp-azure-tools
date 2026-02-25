package io.github.massimilianopili.mcp.azure.compute;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureAksTools {

    private static final String API_VERSION = "2024-09-01";
    private static final String PROVIDER = "/providers/Microsoft.ContainerService/managedClusters";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureAksTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_aks_clusters",
          description = "Elenca tutti i cluster AKS (Azure Kubernetes Service) nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAksClusters() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> clusters = (List<Map<String, Object>>) response.get("value");
                    return clusters.stream().map(c -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", c.getOrDefault("name", ""));
                        r.put("location", c.getOrDefault("location", ""));
                        r.put("kubernetesVersion", c.containsKey("properties")
                                ? ((Map<String, Object>) c.get("properties")).getOrDefault("kubernetesVersion", "")
                                : "");
                        r.put("provisioningState", c.containsKey("properties")
                                ? ((Map<String, Object>) c.get("properties")).getOrDefault("provisioningState", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista cluster AKS: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_aks_cluster",
          description = "Recupera i dettagli di un cluster AKS Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAksCluster(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del cluster AKS") String clusterName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + clusterName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero cluster AKS: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_get_aks_credentials",
          description = "Recupera le credenziali utente (kubeconfig) per un cluster AKS Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAksCredentials(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del cluster AKS") String clusterName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/"
                        + clusterName + "/listClusterUserCredential?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero credenziali AKS: " + e.getMessage())));
    }
}
