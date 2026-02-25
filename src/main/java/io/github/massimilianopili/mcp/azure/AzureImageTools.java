package io.github.massimilianopili.mcp.azure;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AzureImageTools {

    private static final String GALLERY_API = "2023-07-03";
    private static final String IMAGE_API   = "2024-07-01";

    private final WebClient w;
    private final AzureProperties props;

    public AzureImageTools(@Qualifier("azureArmWebClient") WebClient w, AzureProperties props) {
        this.w = w;
        this.props = props;
    }

    @ReactiveTool(name = "azure_list_compute_galleries",
          description = "Elenca tutte le Azure Compute Gallery (Shared Image Gallery) nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listComputeGalleries() {
        return w.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Compute/galleries?api-version=" + GALLERY_API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(g -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", g.getOrDefault("name", ""));
                        r.put("location", g.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) g.getOrDefault("properties", Map.of());
                        r.put("description", p.getOrDefault("description", ""));
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_gallery_images",
          description = "Elenca le definizioni di immagine di una Azure Compute Gallery")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listGalleryImages(
            @ToolParam(description = "Nome del resource group") String resourceGroup,
            @ToolParam(description = "Nome della gallery") String galleryName) {
        return w.get()
                .uri(props.getArmBase() + "/resourceGroups/" + resourceGroup + "/providers/Microsoft.Compute/galleries/" + galleryName + "/images?api-version=" + GALLERY_API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(img -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", img.getOrDefault("name", ""));
                        Map<String, Object> p = (Map<String, Object>) img.getOrDefault("properties", Map.of());
                        r.put("osType", p.getOrDefault("osType", ""));
                        r.put("osState", p.getOrDefault("osState", ""));
                        r.put("hyperVGeneration", p.getOrDefault("hyperVGeneration", ""));
                        Map<String, Object> id = (Map<String, Object>) p.getOrDefault("identifier", Map.of());
                        r.put("publisher", id.getOrDefault("publisher", ""));
                        r.put("offer", id.getOrDefault("offer", ""));
                        r.put("sku", id.getOrDefault("sku", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }

    @ReactiveTool(name = "azure_list_custom_images",
          description = "Elenca le immagini VM personalizzate (non gallery) nella subscription")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listCustomImages() {
        return w.get()
                .uri(props.getArmBase() + "/providers/Microsoft.Compute/images?api-version=" + IMAGE_API)
                .retrieve().bodyToMono(Map.class)
                .map(res -> {
                    if (!res.containsKey("value")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) res.get("value");
                    return items.stream().map(img -> {
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("name", img.getOrDefault("name", ""));
                        r.put("location", img.getOrDefault("location", ""));
                        Map<String, Object> p = (Map<String, Object>) img.getOrDefault("properties", Map.of());
                        r.put("provisioningState", p.getOrDefault("provisioningState", ""));
                        Map<String, Object> sp = (Map<String, Object>) p.getOrDefault("storageProfile", Map.of());
                        Map<String, Object> od = (Map<String, Object>) sp.getOrDefault("osDisk", Map.of());
                        r.put("osType", od.getOrDefault("osType", ""));
                        r.put("osState", od.getOrDefault("osState", ""));
                        return r;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", e.getMessage()))));
    }
}
