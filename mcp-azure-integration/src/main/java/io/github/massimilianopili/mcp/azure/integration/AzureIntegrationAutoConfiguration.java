package io.github.massimilianopili.mcp.azure.integration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.azure.client-id")
@Import({
        AzureApiManagementTools.class, AzureLogicAppsTools.class, AzureCognitiveServicesTools.class,
        AzureSearchTools.class, AzureMLTools.class, AzureDataFactoryTools.class,
        AzureDatabricksTools.class, AzureSynapseTools.class, AzureStreamAnalyticsTools.class,
        AzureAppConfigurationTools.class
})
public class AzureIntegrationAutoConfiguration {
}
