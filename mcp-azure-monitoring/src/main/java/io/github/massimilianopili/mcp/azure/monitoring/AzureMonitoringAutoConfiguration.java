package io.github.massimilianopili.mcp.azure.monitoring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.azure.client-id")
@Import({
        AzureMonitorTools.class, AzureDiagnosticTools.class, AzureAlertTools.class,
        AzureAutoscaleTools.class, AzureCostTools.class
})
public class AzureMonitoringAutoConfiguration {
}
