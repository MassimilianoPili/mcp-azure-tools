package io.github.massimilianopili.mcp.azure.messaging;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureEventHubTools {

    private static final String API_VERSION = "2024-01-01";
    private static final String PROVIDER    = "/providers/Microsoft.EventHub/namespaces";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureEventHubTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_eventhub_namespaces",
          description = "Elenca tutti i namespace Azure Event Hubs nella subscription (streaming di eventi in tempo reale)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listEventHubNamespaces() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> nss = (List<Map<String, Object>>) response.get("value");
                    return nss.stream().map(ns -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ns.getOrDefault("name", ""));
                        r.put("location", ns.getOrDefault("location", ""));
                        Map<String, Object> sku = (Map<String, Object>) ns.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) ns.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("serviceBusEndpoint", p.getOrDefault("serviceBusEndpoint", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Event Hub namespaces: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_eventhub_namespace",
          description = "Recupera i dettagli di un namespace Azure Event Hubs")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getEventHubNamespace(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del namespace Event Hub") String namespaceName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + namespaceName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Event Hub namespace: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_event_hubs",
          description = "Elenca gli Event Hub in un namespace Azure Event Hubs")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listEventHubs(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del namespace Event Hub") String namespaceName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + namespaceName + "/eventhubs?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> hubs = (List<Map<String, Object>>) response.get("value");
                    return hubs.stream().map(hub -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", hub.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) hub.getOrDefault("properties", Map.of());
                        r.put("partitionCount", p.getOrDefault("partitionCount", 0));
                        r.put("messageRetentionInDays", p.getOrDefault("messageRetentionInDays", 0));
                        r.put("status", p.getOrDefault("status", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Event Hubs: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_eventhub_consumer_groups",
          description = "Elenca i consumer group di un Event Hub Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listEventHubConsumerGroups(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del namespace Event Hub") String namespaceName,
            @ToolParam(description = "Nome dell'Event Hub") String eventHubName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + namespaceName
                        + "/eventhubs/" + eventHubName + "/consumergroups?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> cgs = (List<Map<String, Object>>) response.get("value");
                    return cgs.stream().map(cg -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", cg.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) cg.getOrDefault("properties", Map.of());
                        r.put("createdAt", p.getOrDefault("createdAt", ""));
                        r.put("userMetadata", p.getOrDefault("userMetadata", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Event Hub consumer groups: " + e.getMessage()))));
    }
}
