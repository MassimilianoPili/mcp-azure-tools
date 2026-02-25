package io.github.massimilianopili.mcp.azure.compute;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.azure.client-id")
@Import({
        AzureVmTools.class, AzureVmssTools.class, AzureManagedDiskTools.class,
        AzureImageTools.class, AzureAksTools.class, AzureContainerAppTools.class,
        AzureContainerInstanceTools.class, AzureAcrTools.class, AzureAppServiceTools.class,
        AzureFunctionTools.class, AzureStaticWebAppsTools.class, AzureSpringAppsTools.class
})
public class AzureComputeAutoConfiguration {
}
