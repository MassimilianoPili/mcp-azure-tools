package io.github.massimilianopili.mcp.azure.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.azure.client-id")
@Import({
        AzureKeyVaultTools.class, AzureRbacTools.class, AzureManagedIdentityTools.class,
        AzureDefenderTools.class, AzurePolicyTools.class, AzureAdTools.class
})
public class AzureSecurityAutoConfiguration {
}
