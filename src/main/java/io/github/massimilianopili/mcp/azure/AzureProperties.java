package io.github.massimilianopili.mcp.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.azure")
public class AzureProperties {

    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String subscriptionId;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }

    /** Base URL ARM scoped alla subscription corrente */
    public String getArmBase() {
        return "https://management.azure.com/subscriptions/" + subscriptionId;
    }

    /** URL endpoint token OAuth2 per questo tenant */
    public String getTokenUrl() {
        return "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
    }

    /** Base URL Microsoft Graph */
    public String getGraphBase() {
        return "https://graph.microsoft.com/v1.0";
    }
}
