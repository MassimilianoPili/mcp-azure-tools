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
public class AzureCostTools {

    private static final String API_VERSION = "2024-08-01";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureCostTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_get_cost_summary",
          description = "Recupera il riepilogo dei costi mese corrente per la subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getCostSummary() {
        Map<String, Object> body = buildCostQueryBody();
        return webClient.post()
                .uri("https://management.azure.com/subscriptions/" + props.getSubscriptionId()
                        + "/providers/Microsoft.CostManagement/query?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero costi subscription: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_get_cost_by_resource_group",
          description = "Recupera i costi mese corrente per un resource group Azure specifico")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getCostByResourceGroup(
            @ToolParam(description = "Nome del resource group") String resourceGroup) {
        Map<String, Object> body = buildCostQueryBody();
        return webClient.post()
                .uri("https://management.azure.com/subscriptions/" + props.getSubscriptionId()
                        + "/resourceGroups/" + resourceGroup
                        + "/providers/Microsoft.CostManagement/query?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero costi resource group: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_get_usage_details",
          description = "Recupera i dettagli di utilizzo e consumo (ultime 100 voci) per la subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getUsageDetails() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Consumption/usageDetails?$top=100&api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero dettagli utilizzo: " + e.getMessage())));
    }

    private Map<String, Object> buildCostQueryBody() {
        Map<String, Object> totalCost = new LinkedHashMap<>();
        totalCost.put("name", "Cost");
        totalCost.put("function", "Sum");

        Map<String, Object> aggregation = new LinkedHashMap<>();
        aggregation.put("totalCost", totalCost);

        Map<String, Object> dataset = new LinkedHashMap<>();
        dataset.put("granularity", "None");
        dataset.put("aggregation", aggregation);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "ActualCost");
        body.put("timeframe", "MonthToDate");
        body.put("dataset", dataset);
        return body;
    }
}
