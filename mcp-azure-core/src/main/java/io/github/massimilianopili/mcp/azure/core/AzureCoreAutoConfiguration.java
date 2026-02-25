package io.github.massimilianopili.mcp.azure.core;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.azure.client-id")
@EnableConfigurationProperties(AzureProperties.class)
@Import({
        AzureConfig.class,
        AzureSubscriptionTools.class,
        AzureResourceGroupTools.class,
        AzureTagTools.class,
        AzureDeploymentTools.class,
        AzureLockTools.class
})
public class AzureCoreAutoConfiguration {
}
