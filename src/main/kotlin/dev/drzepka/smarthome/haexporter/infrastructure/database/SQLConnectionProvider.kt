package dev.drzepka.smarthome.haexporter.infrastructure.database

import dev.drzepka.smarthome.haexporter.infrastructure.properties.SQLDataSourceProperties
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.ValidationDepth
import kotlinx.coroutines.reactive.awaitSingle
import org.apache.logging.log4j.kotlin.Logging
import java.time.Duration

class SQLConnectionProvider(properties: SQLDataSourceProperties) {

    private val pool: ConnectionPool

    init {
        logger.info { "Creating SQL connection pool with ${properties.copy(password = "***")}" }
        val options = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, properties.driver)
            .option(ConnectionFactoryOptions.HOST, properties.host)
            .option(ConnectionFactoryOptions.PORT, properties.port)
            .option(ConnectionFactoryOptions.USER, properties.username)
            .option(ConnectionFactoryOptions.PASSWORD, properties.password)
            .option(ConnectionFactoryOptions.DATABASE, properties.database)
            .build()

        val factory = ConnectionFactories.get(options)
        val poolConfig = ConnectionPoolConfiguration.builder(factory)
            .maxIdleTime(Duration.ofSeconds(2))
            .maxSize(10)
            .validationQuery("SELECT 1")
            .validationDepth(ValidationDepth.REMOTE)
            .maxAcquireTime(Duration.ofSeconds(1))
            .build()

        pool = ConnectionPool(poolConfig)
    }

    suspend fun getConnection(): Connection = pool.create().awaitSingle()

    companion object : Logging
}
