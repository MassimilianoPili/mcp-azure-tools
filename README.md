# MCP Azure Tools

Multi-module Spring Boot starter providing ~64 MCP tools for Azure Cloud management. Covers compute, networking, data, messaging, security, monitoring, and integrations via direct Azure REST API calls (ARM, Graph, Key Vault) — no Azure SDK required.

## Installation

Use the aggregator module to include all tools:

```xml
<dependency>
    <groupId>io.github.massimilianopili</groupId>
    <artifactId>mcp-azure-all</artifactId>
    <version>0.0.1</version>
</dependency>
```

Or include individual modules: `mcp-azure-core`, `mcp-azure-compute`, `mcp-azure-network`, `mcp-azure-data`, `mcp-azure-messaging`, `mcp-azure-security`, `mcp-azure-monitoring`, `mcp-azure-integration`.

Requires Java 17+, Spring AI 1.0.0+, and [spring-ai-reactive-tools](https://github.com/MassimilianoPili/spring-ai-reactive-tools) 0.2.0+.

## Modules

| Module | Description | Key Tools |
|--------|-------------|-----------|
| **core** | Subscription, resource groups, tags, deployments, locks | 16 tools |
| **compute** | VM, VMSS, AKS, App Service, Functions, Container Apps/Instances, ACR | 12 classes |
| **network** | VNet, DNS, Private DNS, Load Balancer, App Gateway, Front Door, CDN, Firewall | 14 classes |
| **data** | SQL, PostgreSQL, MySQL, CosmosDB, Storage, Redis Cache, Backup | 7 classes |
| **messaging** | Service Bus, Event Grid, Event Hub, SignalR, IoT Hub | 5 classes |
| **security** | Key Vault, RBAC, Managed Identity, Defender, Policy, Azure AD | 6 classes |
| **monitoring** | Log Analytics, Diagnostics, Alerts, Autoscale, Cost Management | 5 classes |
| **integration** | API Management, Logic Apps, Cognitive Services, Search, ML, Data Factory | 10 classes |

## Configuration

```properties
# Required — enables all Azure tools
MCP_AZURE_TENANT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
MCP_AZURE_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
MCP_AZURE_CLIENT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
MCP_AZURE_SUBSCRIPTION_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

Create a Service Principal: Azure AD > App registrations > New registration > Certificates & secrets.

## How It Works

- Uses `@ReactiveTool` ([spring-ai-reactive-tools](https://github.com/MassimilianoPili/spring-ai-reactive-tools)) for async `Mono<T>` methods
- Auto-configured with `@ConditionalOnProperty(name = "mcp.azure.client-id")` on all modules
- **No Azure SDK** — direct REST calls via WebClient (lightweight, minimal dependencies)
- OAuth2 client credentials flow with `AzureTokenService` managing 3 scopes: ARM, Graph, Key Vault
- 3 separate WebClient beans: `azureArmWebClient`, `azureGraphWebClient`, `azureKvWebClient`
- All modules depend on `mcp-azure-core` for properties, config, and token service

## Requirements

- Java 17+
- Spring Boot 3.4+ with WebFlux
- Spring AI 1.0.0+
- spring-ai-reactive-tools 0.2.0+

## License

[MIT License](LICENSE)
