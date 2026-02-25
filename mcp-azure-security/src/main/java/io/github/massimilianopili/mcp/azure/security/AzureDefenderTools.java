package io.github.massimilianopili.mcp.azure.security;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureDefenderTools {

    private static final String ALERTS_API_VERSION      = "2022-01-01";
    private static final String SCORES_API_VERSION      = "2020-01-01";
    private static final String ASSESSMENTS_API_VERSION = "2021-06-01";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureDefenderTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_security_alerts",
          description = "Elenca gli alert di sicurezza attivi in Microsoft Defender for Cloud (threats, misconfigurations)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSecurityAlerts() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Security/alerts?api-version=" + ALERTS_API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> alerts = (List<Map<String, Object>>) response.get("value");
                    return alerts.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", a.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) a.getOrDefault("properties", Map.of());
                        r.put("alertDisplayName", p.getOrDefault("alertDisplayName", ""));
                        r.put("severity", p.getOrDefault("severity", ""));
                        r.put("status", p.getOrDefault("status", ""));
                        r.put("startTimeUtc", p.getOrDefault("startTimeUtc", ""));
                        r.put("description", p.getOrDefault("description", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista security alerts: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_secure_scores",
          description = "Elenca i Secure Score di Microsoft Defender for Cloud (punteggio sicurezza della subscription)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSecureScores() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Security/secureScores?api-version=" + SCORES_API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> scores = (List<Map<String, Object>>) response.get("value");
                    return scores.stream().map(s -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", s.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) s.getOrDefault("properties", Map.of());
                        r.put("displayName", p.getOrDefault("displayName", ""));
                        Map<String, Object> score = (Map<String, Object>) p.getOrDefault("score", Map.of());
                        r.put("current", score.getOrDefault("current", 0));
                        r.put("max", score.getOrDefault("max", 0));
                        r.put("percentage", score.getOrDefault("percentage", 0));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista secure scores: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_security_assessments",
          description = "Elenca le security assessment di Defender for Cloud (raccomandazioni per migliorare la sicurezza)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSecurityAssessments() {
        return webClient.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Security/assessments?api-version=" + ASSESSMENTS_API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> assessments = (List<Map<String, Object>>) response.get("value");
                    return assessments.stream().limit(50).map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        Map<String, Object> p = (Map<String, Object>) a.getOrDefault("properties", Map.of());
                        Map<String, Object> meta = (Map<String, Object>) p.getOrDefault("displayName", Map.of());
                        r.put("displayName", p.getOrDefault("displayName", a.getOrDefault("name", "")));
                        Map<String, Object> status = (Map<String, Object>) p.getOrDefault("status", Map.of());
                        r.put("code", status.getOrDefault("code", ""));
                        r.put("cause", status.getOrDefault("cause", ""));
                        r.put("resourceId", ((String) a.getOrDefault("id", "")).replaceAll("/providers/Microsoft.Security/assessments/.*", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista security assessments: " + e.getMessage()))));
    }
}
