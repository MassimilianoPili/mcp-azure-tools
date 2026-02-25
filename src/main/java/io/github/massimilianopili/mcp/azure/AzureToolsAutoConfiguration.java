package io.github.massimilianopili.mcp.azure;

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
        AzureVmTools.class,
        AzureStorageTools.class,
        AzureAksTools.class,
        AzureAppServiceTools.class,
        AzureKeyVaultTools.class,
        AzureAcrTools.class,
        AzureNetworkTools.class,
        AzureDnsTools.class,
        AzureSqlTools.class,
        AzureCosmosDbTools.class,
        AzureServiceBusTools.class,
        AzureContainerAppTools.class,
        AzureFunctionTools.class,
        AzureRedisCacheTools.class,
        AzurePolicyTools.class,
        AzureLockTools.class,
        AzureCostTools.class,
        AzureTagTools.class,
        AzureAdTools.class,
        AzureRbacTools.class,
        AzurePostgresTools.class,
        AzureAppGatewayTools.class,
        AzureFrontDoorTools.class,
        AzureMonitorTools.class,
        AzureBastionTools.class,
        AzurePrivateDnsTools.class,
        AzureContainerInstanceTools.class,
        AzureDiagnosticTools.class,
        AzureManagedIdentityTools.class,
        AzurePrivateEndpointTools.class,
        AzureNatGatewayTools.class,
        AzureDeploymentTools.class,
        AzureBackupTools.class,
        AzureFirewallTools.class,
        AzureApiManagementTools.class,
        AzureEventGridTools.class,
        AzureEventHubTools.class,
        AzureMySqlTools.class,
        AzureStaticWebAppsTools.class,
        AzureLogicAppsTools.class,
        AzureCognitiveServicesTools.class,
        AzureDefenderTools.class,
        AzureLoadBalancerTools.class,
        AzureVmssTools.class,
        AzureAutoscaleTools.class,
        AzureTrafficManagerTools.class,
        AzureVpnGatewayTools.class,
        AzureManagedDiskTools.class,
        AzureAppConfigurationTools.class,
        AzureAlertTools.class,
        AzureSearchTools.class,
        AzureDataFactoryTools.class,
        AzureIotHubTools.class,
        AzureSignalRTools.class,
        AzureDatabricksTools.class,
        AzureSynapseTools.class,
        AzureCdnTools.class,
        AzureMLTools.class,
        AzureSpringAppsTools.class,
        AzureStreamAnalyticsTools.class,
        AzureImageTools.class,
        AzureExpressRouteTools.class
})
public class AzureToolsAutoConfiguration {
    // Tool registrati automaticamente da ReactiveToolAutoConfiguration di spring-ai-reactive-tools
}
