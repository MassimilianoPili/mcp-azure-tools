package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Tool per Azure Cognitive Services e Azure OpenAI.
 * Gli account Azure OpenAI sono account Cognitive Services con kind="OpenAI".
 * I deployment di modelli (gpt-4, gpt-4o, etc.) sono gestiti a livello di account.
 */
@Service
public class AzureCognitiveServicesTools {

    private static final String API_VERSION = "2024-10-01";
    private static final String PROVIDER    = "/providers/Microsoft.CognitiveServices/accounts";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureCognitiveServicesTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_cognitive_accounts",
          description = "Elenca tutti gli account Azure Cognitive Services e Azure OpenAI nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listCognitiveAccounts() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> accounts = (List<Map<String, Object>>) response.get("value");
                    return accounts.stream().map(acc -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", acc.getOrDefault("name", ""));
                        r.put("location", acc.getOrDefault("location", ""));
                        r.put("kind", acc.getOrDefault("kind", ""));
                        Map<String, Object> sku = (Map<String, Object>) acc.getOrDefault("sku", Map.of());
                        r.put("sku", sku.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) acc.getOrDefault("properties", Map.of());
                        r.put("endpoint", p.getOrDefault("endpoint", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista Cognitive Services accounts: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_cognitive_account",
          description = "Recupera i dettagli di un account Azure Cognitive Services o Azure OpenAI")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getCognitiveAccount(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'account Cognitive Services") String accountName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + accountName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero Cognitive Services account: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_openai_deployments",
          description = "Elenca i deployment di modelli in un account Azure OpenAI (gpt-4o, gpt-4, text-embedding, etc.)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listOpenAiDeployments(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'account Azure OpenAI") String accountName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + accountName + "/deployments?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> deps = (List<Map<String, Object>>) response.get("value");
                    return deps.stream().map(d -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", d.getOrDefault("name", ""));
                        Map<String, Object> sku = (Map<String, Object>) d.getOrDefault("sku", Map.of());
                        r.put("capacity", sku.getOrDefault("capacity", 0));
                        Map<String, Object> p = (Map<String, Object>) d.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        Map<String, Object> model = (Map<String, Object>) p.getOrDefault("model", Map.of());
                        r.put("model", model.getOrDefault("name", ""));
                        r.put("modelVersion", model.getOrDefault("version", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista OpenAI deployments: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_openai_models",
          description = "Elenca i modelli disponibili per il deployment in un account Azure OpenAI")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listOpenAiModels(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'account Azure OpenAI") String accountName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + accountName + "/models?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("value");
                    return models.stream().map(m -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", m.getOrDefault("name", ""));
                        r.put("version", m.getOrDefault("version", ""));
                        r.put("lifecycleStatus", m.getOrDefault("lifecycleStatus", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista OpenAI models: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_cognitive_account_keys",
          description = "Recupera le API key di un account Azure Cognitive Services o Azure OpenAI")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> listCognitiveAccountKeys(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome dell'account Cognitive Services") String accountName) {
        return webClient.post()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + accountName + "/listKeys?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero API keys Cognitive Services: " + e.getMessage())));
    }
}
