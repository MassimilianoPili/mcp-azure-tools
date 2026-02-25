package io.github.massimilianopili.mcp.azure.network;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.azure.client-id")
@Import({
        AzureNetworkTools.class, AzureDnsTools.class, AzurePrivateDnsTools.class,
        AzureLoadBalancerTools.class, AzureAppGatewayTools.class, AzureFrontDoorTools.class,
        AzureCdnTools.class, AzureFirewallTools.class, AzureNatGatewayTools.class,
        AzureBastionTools.class, AzureTrafficManagerTools.class, AzureVpnGatewayTools.class,
        AzureExpressRouteTools.class, AzurePrivateEndpointTools.class
})
public class AzureNetworkAutoConfiguration {
}
