package io.github.massimilianopili.mcp.azure.security;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureAdTools {

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureAdTools(
            @Qualifier("azureGraphWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_ad_users",
          description = "Elenca gli utenti Azure Active Directory (prime 100 voci)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAdUsers() {
        return webClient.get()
                .uri(props.getGraphBase() + "/users?$select=id,displayName,mail,userPrincipalName&$top=100")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> users = (List<Map<String, Object>>) response.get("value");
                    return users.stream().map(u -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("id", u.getOrDefault("id", ""));
                        r.put("displayName", u.getOrDefault("displayName", ""));
                        r.put("mail", u.getOrDefault("mail", ""));
                        r.put("userPrincipalName", u.getOrDefault("userPrincipalName", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista utenti AD: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_ad_user",
          description = "Recupera i dettagli di un utente Azure Active Directory per ID o UPN")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getAdUser(
            @ToolParam(description = "ID o User Principal Name (UPN) dell'utente, es: utente@dominio.com") String userId) {
        return webClient.get()
                .uri(props.getGraphBase() + "/users/" + userId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero utente AD: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_ad_groups",
          description = "Elenca i gruppi Azure Active Directory (prime 100 voci)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAdGroups() {
        return webClient.get()
                .uri(props.getGraphBase() + "/groups?$select=id,displayName,mail&$top=100")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> groups = (List<Map<String, Object>>) response.get("value");
                    return groups.stream().map(g -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("id", g.getOrDefault("id", ""));
                        r.put("displayName", g.getOrDefault("displayName", ""));
                        r.put("mail", g.getOrDefault("mail", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista gruppi AD: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_service_principals",
          description = "Elenca i service principal (app identity) in Azure Active Directory")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listServicePrincipals() {
        return webClient.get()
                .uri(props.getGraphBase() + "/servicePrincipals?$select=id,displayName,appId&$top=100")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> sps = (List<Map<String, Object>>) response.get("value");
                    return sps.stream().map(sp -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("id", sp.getOrDefault("id", ""));
                        r.put("displayName", sp.getOrDefault("displayName", ""));
                        r.put("appId", sp.getOrDefault("appId", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista service principal: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_app_registrations",
          description = "Elenca le app registration in Azure Active Directory")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAppRegistrations() {
        return webClient.get()
                .uri(props.getGraphBase() + "/applications?$select=id,displayName,appId&$top=100")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> apps = (List<Map<String, Object>>) response.get("value");
                    return apps.stream().map(a -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("id", a.getOrDefault("id", ""));
                        r.put("displayName", a.getOrDefault("displayName", ""));
                        r.put("appId", a.getOrDefault("appId", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista app registration: " + e.getMessage()))));
    }
}
