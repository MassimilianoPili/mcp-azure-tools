package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureLogicAppsTools {

    private static final String API_VERSION = "2019-05-01";
    private static final String PROVIDER    = "/providers/Microsoft.Logic/workflows";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureLogicAppsTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_logic_apps",
          description = "Elenca tutti i Logic App (workflow) nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listLogicApps() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> apps = (List<Map<String, Object>>) response.get("value");
                    return apps.stream().map(app -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", app.getOrDefault("name", ""));
                        r.put("location", app.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) app.getOrDefault("properties", Map.of());
                        r.put("state", p.getOrDefault("state", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        r.put("createdTime", p.getOrDefault("createdTime", ""));
                        r.put("changedTime", p.getOrDefault("changedTime", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Logic Apps: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_logic_app",
          description = "Recupera i dettagli di un Logic App Azure (definizione workflow)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getLogicApp(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del Logic App") String workflowName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + workflowName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Logic App: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_enable_logic_app",
          description = "Abilita (attiva) un Logic App Azure disabilitato")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> enableLogicApp(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del Logic App da abilitare") String workflowName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + workflowName + "/enable?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Abilitato: " + workflowName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore abilitazione Logic App: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_disable_logic_app",
          description = "Disabilita un Logic App Azure (ferma le esecuzioni future)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> disableLogicApp(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome del Logic App da disabilitare") String workflowName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + workflowName + "/disable?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .defaultIfEmpty(Map.of("status", "Disabilitato: " + workflowName))
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore disabilitazione Logic App: " + e.getMessage())));
    }
}
