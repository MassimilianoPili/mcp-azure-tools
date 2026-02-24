package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureSubscriptionTools {

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureSubscriptionTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_subscriptions",
          description = "Elenca tutte le subscription Azure accessibili con il Service Principal configurato")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSubscriptions() {
        return webClient.get()
                .uri("https://management.azure.com/subscriptions?api-version=2022-12-01")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> subs = (List<Map<String, Object>>) response.get("value");
                    return subs.stream().map(s -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("subscriptionId", s.getOrDefault("subscriptionId", ""));
                        r.put("displayName", s.getOrDefault("displayName", ""));
                        r.put("state", s.getOrDefault("state", ""));
                        r.put("tenantId", s.getOrDefault("tenantId", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista subscription: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_subscription",
          description = "Recupera i dettagli della subscription Azure corrente configurata")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getSubscription(
            @ToolParam(description = "ID subscription (lascia vuoto per usare quella configurata)", required = false) String subscriptionId) {
        String subId = (subscriptionId != null && !subscriptionId.isBlank()) ? subscriptionId : props.getSubscriptionId();
        return webClient.get()
                .uri("https://management.azure.com/subscriptions/" + subId + "?api-version=2022-12-01")
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero subscription: " + e.getMessage())));
    }
}
