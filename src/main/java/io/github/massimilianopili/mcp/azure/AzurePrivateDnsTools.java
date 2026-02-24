package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzurePrivateDnsTools {

    private static final String API_VERSION      = "2020-06-01";
    private static final String PROVIDER         = "/providers/Microsoft.Network/privateDnsZones";
    private static final String MGMT_BASE        = "https://management.azure.com";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzurePrivateDnsTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_private_dns_zones",
          description = "Elenca tutte le zone DNS private nella subscription Azure (per risoluzione interna tra servizi)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPrivateDnsZones() {
        return webClient.get()
                .uri(props.getArmBase() + PROVIDER + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> zones = (List<Map<String, Object>>) response.get("value");
                    return zones.stream().map(z -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", z.getOrDefault("name", ""));
                        r.put("location", z.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) z.getOrDefault("properties", Map.of());
                        r.put("numberOfRecordSets", p.getOrDefault("numberOfRecordSets", 0));
                        r.put("numberOfVirtualNetworkLinks", p.getOrDefault("numberOfVirtualNetworkLinks", 0));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista zone DNS private: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_private_dns_zone",
          description = "Recupera i dettagli di una zona DNS privata Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getPrivateDnsZone(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della zona DNS privata, es: internal.contoso.com") String zoneName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + zoneName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero zona DNS privata: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_private_dns_records",
          description = "Elenca i record set di una zona DNS privata Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPrivateDnsRecords(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della zona DNS privata") String zoneName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + zoneName + "/ALL?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> records = (List<Map<String, Object>>) response.get("value");
                    return records.stream().map(rec -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", rec.getOrDefault("name", ""));
                        r.put("type", ((String) rec.getOrDefault("type", "")).replaceAll(".*/", ""));
                        Map<String, Object> p = (Map<String, Object>) rec.getOrDefault("properties", Map.of());
                        r.put("ttl", p.getOrDefault("ttl", 0));
                        r.put("fqdn", p.getOrDefault("fqdn", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista record DNS privati: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_create_private_dns_record",
          description = "Crea o aggiorna un record A in una zona DNS privata Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createPrivateDnsRecord(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della zona DNS privata") String zoneName,
            @ToolParam(description = "Nome del record, es: myservice") String recordName,
            @ToolParam(description = "Indirizzo IP del record A") String ipAddress,
            @ToolParam(description = "TTL in secondi (default 300)") int ttl) {
        Map<String, Object> body = Map.of(
                "properties", Map.of(
                        "ttl", ttl,
                        "aRecords", List.of(Map.of("ipv4Address", ipAddress))
                )
        );
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + zoneName + "/A/" + recordName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione record DNS privato: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_link_private_dns_to_vnet",
          description = "Collega una zona DNS privata a una VNet (abilita la risoluzione DNS interna per quella VNet)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> linkPrivateDnsToVnet(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della zona DNS privata") String zoneName,
            @ToolParam(description = "Nome del link (identificatore univoco)") String linkName,
            @ToolParam(description = "Resource ID della VNet da collegare") String vnetId,
            @ToolParam(description = "Abilita auto-registrazione (true) — registra automaticamente le VM della VNet") boolean enableAutoRegistration) {
        Map<String, Object> body = Map.of(
                "location", "global",
                "properties", Map.of(
                        "registrationEnabled", enableAutoRegistration,
                        "virtualNetwork", Map.of("id", vnetId)
                )
        );
        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + zoneName
                        + "/virtualNetworkLinks/" + linkName + "?api-version=" + API_VERSION)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore link DNS privato-VNet: " + e.getMessage())));
    }
}
