package io.github.massimilianopili.mcp.azure.messaging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.azure.client-id")
@Import({
        AzureServiceBusTools.class, AzureEventGridTools.class, AzureEventHubTools.class,
        AzureSignalRTools.class, AzureIotHubTools.class
})
public class AzureMessagingAutoConfiguration {
}
