package io.github.massimilianopili.mcp.azure.data;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.azure.client-id")
@Import({
        AzureSqlTools.class, AzurePostgresTools.class, AzureMySqlTools.class,
        AzureCosmosDbTools.class, AzureStorageTools.class, AzureRedisCacheTools.class,
        AzureBackupTools.class
})
public class AzureDataAutoConfiguration {
}
