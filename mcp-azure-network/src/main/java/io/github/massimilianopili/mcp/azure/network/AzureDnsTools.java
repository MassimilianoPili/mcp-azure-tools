package io.github.massimilianopili.mcp.azure.network;

import io.github.massimilianopili.mcp.azure.core.AzureProperties;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureDnsTools {

    private static final String API_VERSION = "2018-05-01";
    private static final String PROVIDER    = "/providers/Microsoft.Network/dnsZones";

    private final WebClient webClient;
    private final AzureProperties props;

    public AzureDnsTools(
            @Qualifier("azureArmWebClient") WebClient webClient,
            AzureProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_dns_zones",
          description = "Elenca tutte le zone DNS nella subscription Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listDnsZones() {
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
                        r.put("numberOfRecordSets", z.containsKey("properties")
                                ? ((Map<String, Object>) z.get("properties")).getOrDefault("numberOfRecordSets", 0)
                                : 0);
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista zone DNS: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_get_dns_zone",
          description = "Recupera i dettagli di una zona DNS Azure")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getDnsZone(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della zona DNS, es: example.com") String zoneName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + zoneName + "?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero zona DNS: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_list_dns_records",
          description = "Elenca tutti i record DNS di una zona Azure")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listDnsRecords(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della zona DNS, es: example.com") String zoneName) {
        return webClient.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/" + zoneName + "/all?api-version=" + API_VERSION)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> records = (List<Map<String, Object>>) response.get("value");
                    return records.stream().map(rec -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", rec.getOrDefault("name", ""));
                        r.put("type", ((String) rec.getOrDefault("type", "")).replaceAll(".*/", ""));
                        r.put("ttl", rec.containsKey("properties")
                                ? ((Map<String, Object>) rec.get("properties")).getOrDefault("TTL", 0)
                                : 0);
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore lista record DNS: " + e.getMessage()))));
    }

    @ReactiveTool(name = "azure_create_dns_record",
          description = "Crea o aggiorna un record DNS in una zona Azure. Tipi supportati: A, AAAA, CNAME, MX, TXT, NS, PTR")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createDnsRecord(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della zona DNS, es: example.com") String zoneName,
            @ToolParam(description = "Tipo di record: A, AAAA, CNAME, MX, TXT") String recordType,
            @ToolParam(description = "Nome del record, es: www o @ per root") String name,
            @ToolParam(description = "TTL in secondi (default: 3600)", required = false) Integer ttl,
            @ToolParam(description = "Valore del record. Per A: IP address. Per CNAME: target hostname. Per TXT: testo. Per MX: 'preference hostname'") String value) {

        int ttlValue = (ttl != null) ? ttl : 3600;
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("TTL", ttlValue);

        switch (recordType.toUpperCase()) {
            case "A"    -> properties.put("ARecords", List.of(Map.of("ipv4Address", value)));
            case "AAAA" -> properties.put("AAAARecords", List.of(Map.of("ipv6Address", value)));
            case "CNAME"-> properties.put("CNAMERecord", Map.of("cname", value));
            case "TXT"  -> properties.put("TXTRecords", List.of(Map.of("value", List.of(value))));
            case "MX"   -> {
                String[] parts = value.split(" ", 2);
                properties.put("MXRecords", List.of(Map.of(
                        "preference", Integer.parseInt(parts[0].trim()),
                        "exchange", parts.length > 1 ? parts[1].trim() : parts[0].trim()
                )));
            }
            default -> properties.put("value", value);
        }

        return webClient.put()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/"
                        + zoneName + "/" + recordType + "/" + name + "?api-version=" + API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("properties", properties))
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione record DNS: " + e.getMessage())));
    }

    @ReactiveTool(name = "azure_delete_dns_record",
          description = "Elimina un record DNS da una zona Azure")
    public Mono<Map<String, Object>> deleteDnsRecord(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della zona DNS, es: example.com") String zoneName,
            @ToolParam(description = "Tipo di record: A, AAAA, CNAME, MX, TXT") String recordType,
            @ToolParam(description = "Nome del record, es: www") String name) {
        return webClient.delete()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + PROVIDER + "/"
                        + zoneName + "/" + recordType + "/" + name + "?api-version=" + API_VERSION)
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "deleted", true))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione record DNS: " + e.getMessage())));
    }
}
