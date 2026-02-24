package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureServiceBusTools {

    private static final String API_VERSION = "2022-10-01-preview";
    private static final String PROVIDER    = "/providers/Microsoft.ServiceBus/namespaces";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureServiceBusTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_servicebus_namespaces",
          description = "Elenca tutti i namespace Azure Service Bus nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listServiceBusNamespaces() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> namespaces = (List<Map<String, Object>>) response.get("value");
                    return namespaces.stream().map(ns -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", ns.getOrDefault("name", ""));
                        r.put("location", ns.getOrDefault("location", ""));
                        r.put("sku", ns.containsKey("sku") ? ((Map<String, Object>) ns.get("sku")).getOrDefault("name", "") : "");
                        r.put("status", ns.containsKey("properties")
                                ? ((Map<String, Object>) ns.get("properties")).getOrDefault("status", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista namespace Service Bus: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_servicebus_queues",
          description = "Elenca le code di un namespace Azure Service Bus")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listServiceBusQueues(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del namespace Service Bus") String namespaceName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + namespaceName
                        + "/queues?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> queues = (List<Map<String, Object>>) response.get("value");
                    return queues.stream().map(q -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", q.getOrDefault("name", ""));
                        r.put("messageCount", q.containsKey("properties")
                                ? ((Map<String, Object>) q.get("properties")).getOrDefault("messageCount", 0)
                                : 0);
                        r.put("status", q.containsKey("properties")
                                ? ((Map<String, Object>) q.get("properties")).getOrDefault("status", "")
                                : "");
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista code Service Bus: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_servicebus_topics",
          description = "Elenca i topic di un namespace Azure Service Bus")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listServiceBusTopics(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del namespace Service Bus") String namespaceName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + namespaceName
                        + "/topics?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> topics = (List<Map<String, Object>>) response.get("value");
                    return topics.stream().map(t -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", t.getOrDefault("name", ""));
                        r.put("subscriptionCount", t.containsKey("properties")
                                ? ((Map<String, Object>) t.get("properties")).getOrDefault("subscriptionCount", 0)
                                : 0);
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista topic Service Bus: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_servicebus_queue",
          description = "Recupera i dettagli e conteggio messaggi di una coda Azure Service Bus")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getServiceBusQueue(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del namespace Service Bus") String namespaceName,
            @ToolParam(description = "Nome della coda") String queueName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + namespaceName
                        + "/queues/" + queueName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero coda Service Bus: " + e.getMessage())));
    }
}
