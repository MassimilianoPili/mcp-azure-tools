# MCP Azure Tools

Libreria multi-modulo Spring Boot che fornisce tool MCP per la gestione di risorse Azure Cloud via REST API (ARM, Graph, Key Vault). Pubblicata su Maven Central come `io.github.massimilianopili:mcp-azure-all` (aggregatore) e moduli singoli.

## Build

```bash
# Build tutti i moduli
/opt/maven/bin/mvn clean compile

# Install locale (senza GPG)
/opt/maven/bin/mvn clean install -Dgpg.skip=true

# Deploy su Maven Central
/opt/maven/bin/mvn clean deploy
```

Java 17+ richiesto. Maven: `/opt/maven/bin/mvn`.

## Struttura Progetto (Multi-Modulo)

```
mcp-azure-tools/
├── pom.xml                          # Parent POM (aggregatore, 9 moduli)
├── mcp-azure-all/                   # Aggrega tutti i moduli in un singolo JAR
├── mcp-azure-core/                  # OAuth2 token, WebClient, subscription, resource groups, tags, lock, deployments
├── mcp-azure-compute/               # VM, VMSS, AKS, App Service, Functions, Container Apps/Instances, ACR, Spring Apps, Static Web Apps
├── mcp-azure-network/               # VNet, DNS, Private DNS, Load Balancer, App Gateway, Front Door, CDN, Firewall, NAT, Bastion, VPN, ExpressRoute
├── mcp-azure-data/                  # SQL, PostgreSQL, MySQL, CosmosDB, Storage, Redis Cache, Backup
├── mcp-azure-messaging/             # Service Bus, Event Grid, Event Hub, SignalR, IoT Hub
├── mcp-azure-security/              # Key Vault, RBAC, Managed Identity, Defender, Policy, Azure AD
├── mcp-azure-monitoring/            # Log Analytics, Diagnostics, Alerts, Autoscale, Cost Management
└── mcp-azure-integration/           # API Management, Logic Apps, Cognitive Services, Search, ML, Data Factory, Databricks, Synapse, Stream Analytics, App Configuration
```

Ogni modulo segue la struttura:
```
src/main/java/io/github/massimilianopili/mcp/azure/{modulo}/
├── Azure*Tools.java                 # @ReactiveTool: tool specifici del dominio
└── Azure{Modulo}AutoConfiguration.java

src/main/resources/META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## Core Module

### AzureProperties (`mcp.azure`)
- `tenantId` — Azure AD tenant ID
- `clientId` — Service Principal app ID (trigger attivazione)
- `clientSecret` — Service Principal secret
- `subscriptionId` — Sottoscrizione target
- Helper: `getArmBase()`, `getTokenUrl()`, `getGraphBase()`

### AzureTokenService
OAuth2 client credentials flow con 3 scope: ARM, Graph, Key Vault. Cache token con refresh automatico (buffer 300s).

### AzureConfig
3 bean WebClient separati:
- `azureArmWebClient` — ARM API (control plane, `management.azure.com`)
- `azureGraphWebClient` — Graph API (`graph.microsoft.com`)
- `azureKvWebClient` — Key Vault data plane (per-vault URL, API v7.4)

## Moduli e Tool

### Core (16 tool)
- **AzureSubscriptionTools** (2) — lista/dettaglio sottoscrizioni
- **AzureResourceGroupTools** (4) — CRUD resource group
- **AzureTagTools** (3) — tag risorse, filtra per tag
- **AzureDeploymentTools** (4) — deploy ARM template (Incremental/Complete)
- **AzureLockTools** (3) — management lock (CanNotDelete/ReadOnly)

### Compute (12 classi)
- **AzureVmTools** — VM lifecycle (list, get, start, stop, restart, status)
- **AzureVmssTools** — Scale Set (list, get, instances, scale)
- **AzureManagedDiskTools** — Dischi gestiti
- **AzureImageTools** — Compute Gallery, custom images
- **AzureAksTools** — AKS cluster (list, get, credentials)
- **AzureContainerAppTools** — Container Apps (list, get, revisions, restart)
- **AzureContainerInstanceTools** — Container Instances
- **AzureAcrTools** — Azure Container Registry
- **AzureAppServiceTools** — Web App (list, get, start, stop, restart)
- **AzureFunctionTools** — Function Apps
- **AzureStaticWebAppsTools** — Static Web Apps
- **AzureSpringAppsTools** — Azure Spring Apps

### Network (14 classi)
- **AzureNetworkTools** — VNet, subnet, NSG, public IP, NIC
- **AzureDnsTools** — Zone DNS, record CRUD (A, AAAA, CNAME, TXT, MX)
- **AzurePrivateDnsTools** — DNS privato
- **AzureLoadBalancerTools** — Load Balancer
- **AzureAppGatewayTools** — Application Gateway
- **AzureFrontDoorTools** — Front Door
- **AzureCdnTools** — CDN
- **AzureFirewallTools** — Azure Firewall
- **AzureNatGatewayTools** — NAT Gateway
- **AzureBastionTools** — Bastion
- **AzureTrafficManagerTools** — Traffic Manager
- **AzureVpnGatewayTools** — VPN Gateway
- **AzureExpressRouteTools** — ExpressRoute
- **AzurePrivateEndpointTools** — Private Endpoint

### Data (7 classi)
- **AzureSqlTools** — SQL Server/Database, firewall rules
- **AzurePostgresTools** — PostgreSQL Flexible Server
- **AzureMySqlTools** — MySQL Flexible Server
- **AzureCosmosDbTools** — Cosmos DB
- **AzureStorageTools** — Storage Account, blob container
- **AzureRedisCacheTools** — Redis Cache
- **AzureBackupTools** — Recovery Services

### Messaging (5 classi)
- **AzureServiceBusTools** — Namespace, queue, topic
- **AzureEventGridTools** — Event Grid
- **AzureEventHubTools** — Event Hub
- **AzureSignalRTools** — SignalR
- **AzureIotHubTools** — IoT Hub

### Security (6 classi)
- **AzureKeyVaultTools** — Secret CRUD (usa `azureKvWebClient` separato)
- **AzureRbacTools** — Role assignment
- **AzureManagedIdentityTools** — Managed Identity
- **AzureDefenderTools** — Defender for Cloud
- **AzurePolicyTools** — Azure Policy
- **AzureAdTools** — Azure AD (via Graph API)

### Monitoring (5 classi)
- **AzureMonitorTools** — Log Analytics workspace, metric alert, action group
- **AzureDiagnosticTools** — Diagnostic settings
- **AzureAlertTools** — Alert rules
- **AzureAutoscaleTools** — Autoscale settings
- **AzureCostTools** — Cost Management

### Integration (10 classi)
- **AzureApiManagementTools** — API Management
- **AzureLogicAppsTools** — Logic Apps
- **AzureCognitiveServicesTools** — Cognitive Services
- **AzureSearchTools** — Cognitive Search
- **AzureMLTools** — Machine Learning
- **AzureDataFactoryTools** — Data Factory
- **AzureDatabricksTools** — Databricks
- **AzureSynapseTools** — Synapse Analytics
- **AzureStreamAnalyticsTools** — Stream Analytics
- **AzureAppConfigurationTools** — App Configuration

## Pattern Chiave

- **@ReactiveTool** (spring-ai-reactive-tools): tutti i tool restituiscono `Mono<T>`.
- **Nessun Azure SDK**: chiamate REST dirette via WebClient (leggero, no dipendenze pesanti).
- **OAuth2 automatico**: `AzureTokenService` gestisce acquisizione e refresh token per 3 scope (ARM, Graph, Key Vault).
- **Multi-WebClient**: bean separati per ARM control plane, Graph API e Key Vault data plane.
- **Attivazione condizionale**: `@ConditionalOnProperty(name = "mcp.azure.client-id")` su tutti i moduli.
- **Ogni modulo dipende da `mcp-azure-core`** per properties, config e token service.

## Configurazione

```properties
# Obbligatorie — abilitano tutti i tool Azure
MCP_AZURE_TENANT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
MCP_AZURE_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
MCP_AZURE_CLIENT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
MCP_AZURE_SUBSCRIPTION_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

Service Principal: Azure AD -> App registrations -> New registration -> Certificates & secrets.

## Dipendenze

- Spring Boot 3.4.1 (spring-boot-autoconfigure, spring-boot-starter-webflux)
- Spring AI 1.0.0 (spring-ai-model)
- spring-ai-reactive-tools 0.2.0

## Maven Central

- GroupId: `io.github.massimilianopili`
- Plugin: `central-publishing-maven-plugin` v0.7.0
- Credenziali: Central Portal token in `~/.m2/settings.xml` (server id: `central`)
