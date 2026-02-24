package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureEventGridTools {

    private static final String API_VERSION = "2024-06-01-preview";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureEventGridTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_event_grid_topics",
          description = "Elenca tutti i topic Event Grid nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listEventGridTopics() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.EventGrid/topics?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> topics = (List<Map<String, Object>>) response.get("value");
                    return topics.stream().map(t -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", t.getOrDefault("name", ""));
                        r.put("location", t.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) t.getOrDefault("properties", Map.of());
                        r.put("endpoint", p.getOrDefault("endpoint", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("inputSchema", p.getOrDefault("inputSchema", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Event Grid topics: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_event_grid_topic",
          description = "Recupera i dettagli di un topic Event Grid Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getEventGridTopic(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del topic Event Grid") String topicName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.EventGrid/topics/" + topicName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Event Grid topic: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_event_subscriptions",
          description = "Elenca tutte le event subscription nella subscription Azure (routing eventi a destinazioni)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listEventSubscriptions() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.EventGrid/eventSubscriptions?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> subs = (List<Map<String, Object>>) response.get("value");
                    return subs.stream().map(s -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", s.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) s.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("topic", p.getOrDefault("topic", ""));
                        Map<String, Object> dest = (Map<String, Object>) p.getOrDefault("destination", Map.of());
                        r.put("destinationType", dest.getOrDefault("endpointType", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista event subscriptions: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_create_event_subscription",
          description = "Crea una event subscription Event Grid per inviare eventi di una risorsa a un endpoint (webhook, Service Bus, Event Hub)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createEventSubscription(
            @ToolParam(description = "Resource ID ARM del scope sorgente (es: resource group, topic)") String scopeId,
            @ToolParam(description = "Nome della event subscription") String subscriptionName,
            @ToolParam(description = "Tipo destinazione: WebHook, ServiceBusQueue, EventHub, StorageQueue") String destinationType,
            @ToolParam(description = "Resource ID o URL della destinazione") String destinationEndpoint) {
        Map<String, Object> destination = new LinkedHashMap<>();
        destination.put("endpointType", destinationType);
        if ("WebHook".equalsIgnoreCase(destinationType)) {
            destination.put("properties", Map.of("endpointUrl", destinationEndpoint));
        } else {
            destination.put("properties", Map.of("resourceId", destinationEndpoint));
        }
        Map<String, Object> body = Map.of(
                "properties", Map.of(
                        "destination", destination,
                        "filter", Map.of("includedEventTypes", List.of("All"))
                )
        );
        return webClient.put()
                .uri("https://management.azure.com" + scopeId
                        + "/providers/Microsoft.EventGrid/eventSubscriptions/" + subscriptionName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione event subscription: " + e.getMessage())));
    }
}
