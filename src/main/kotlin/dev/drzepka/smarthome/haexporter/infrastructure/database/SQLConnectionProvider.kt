package dev.drzepka.smarthome.haexporter.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.drzepka.smarthome.haexporter.infrastructure.properties.SQLDataSourceProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.Logging
import java.sql.Connection

class SQLConnectionProvider(properties: SQLDataSourceProperties) {

    // TODO: gracefully shutdown data source on application exit
    private val dataSource: HikariDataSource

    init {
        logger.info { "Creating SQL connection pool with ${properties.copy(password = "***")}" }
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:${properties.driver}://${properties.host}:${properties.port}/${properties.database}"
        config.username = properties.username
        config.password = properties.password

        config.maximumPoolSize = 10
        config.validationTimeout = 3000
        config.connectionTestQuery = "SELECT 1"

        dataSource = HikariDataSource(config)
    }

    suspend fun <T> acquireConnection(block: suspend (Connection) -> T): T = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            block(connection)
        }
    }

    companion object : Logging
}
