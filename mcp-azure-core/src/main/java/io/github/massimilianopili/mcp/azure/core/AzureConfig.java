package io.github.massimilianopili.mcp.azure.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AzureConfig {

    @Bean
    public AzureTokenService azureTokenService(AzureProperties props) {
        WebClient tokenWebClient = WebClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
        return new AzureTokenService(tokenWebClient, props);
    }

    /** WebClient per Azure Resource Manager (ARM scope) */
    @Bean(name = "azureArmWebClient")
    public WebClient azureArmWebClient(AzureTokenService tokenService) {
        return buildWebClient(tokenService, AzureTokenService.ARM_SCOPE);
    }

    /** WebClient per Microsoft Graph (Graph scope) */
    @Bean(name = "azureGraphWebClient")
    public WebClient azureGraphWebClient(AzureTokenService tokenService) {
        return buildWebClient(tokenService, AzureTokenService.GRAPH_SCOPE);
    }

    /** WebClient per Azure Key Vault data plane (vault scope) */
    @Bean(name = "azureKvWebClient")
    public WebClient azureKvWebClient(AzureTokenService tokenService) {
        return buildWebClient(tokenService, AzureTokenService.KV_SCOPE);
    }

    private WebClient buildWebClient(AzureTokenService tokenService, String scope) {
        ExchangeFilterFunction bearerFilter = ExchangeFilterFunction.ofRequestProcessor(
                req -> tokenService.getToken(scope)
                        .map(token -> ClientRequest.from(req)
                                .header("Authorization", "Bearer " + token)
                                .build())
        );

        return WebClient.builder()
                .filter(bearerFilter)
                .defaultHeader("Accept", "application/json")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                        .build())
                .build();
    }
}
