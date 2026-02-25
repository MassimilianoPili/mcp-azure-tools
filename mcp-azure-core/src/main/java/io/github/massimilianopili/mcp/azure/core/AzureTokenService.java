package io.github.massimilianopili.mcp.azure.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce i token OAuth2 per Azure (ARM, Graph, Key Vault).
 * Cache per scope con refresh anticipato di 5 minuti prima della scadenza.
 */
public class AzureTokenService {

    private static final Logger log = LoggerFactory.getLogger(AzureTokenService.class);

    /** Scope OAuth2 per Azure Resource Manager */
    public static final String ARM_SCOPE   = "https://management.azure.com/.default";
    /** Scope OAuth2 per Microsoft Graph */
    public static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    /** Scope OAuth2 per Azure Key Vault */
    public static final String KV_SCOPE    = "https://vault.azure.net/.default";

    private static final int REFRESH_BUFFER_SECONDS = 300;

    private final WebClient tokenWebClient;
    private final AzureProperties props;
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public AzureTokenService(WebClient tokenWebClient, AzureProperties props) {
        this.tokenWebClient = tokenWebClient;
        this.props = props;
    }

    /**
     * Restituisce un access token valido per il dato scope.
     * Ricicla il token dalla cache se non è prossimo alla scadenza.
     */
    @SuppressWarnings("unchecked")
    public Mono<String> getToken(String scope) {
        CachedToken cached = tokenCache.get(scope);
        if (cached != null && cached.isValid()) {
            return Mono.just(cached.accessToken);
        }

        return tokenWebClient.post()
                .uri(props.getTokenUrl())
                .body(BodyInserters.fromFormData("client_id", props.getClientId())
                        .with("client_secret", props.getClientSecret())
                        .with("grant_type", "client_credentials")
                        .with("scope", scope))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String token = (String) response.get("access_token");
                    int expiresIn = (int) response.getOrDefault("expires_in", 3600);
                    Instant expiry = Instant.now().plusSeconds(expiresIn - REFRESH_BUFFER_SECONDS);
                    tokenCache.put(scope, new CachedToken(token, expiry));
                    log.debug("Token Azure acquisito per scope {}, scade tra {} s", scope, expiresIn - REFRESH_BUFFER_SECONDS);
                    return token;
                });
    }

    private static class CachedToken {
        final String accessToken;
        final Instant expiresAt;

        CachedToken(String accessToken, Instant expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }

        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
