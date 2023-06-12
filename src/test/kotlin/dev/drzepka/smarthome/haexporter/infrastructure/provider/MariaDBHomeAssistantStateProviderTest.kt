package dev.drzepka.smarthome.haexporter.infrastructure.provider

import dev.drzepka.smarthome.haexporter.infrasctucture.properties.SQLDataSourceProperties
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container

class MariaDBHomeAssistantStateProviderTest : BaseSQLHomeAssistantStateProviderTest() {
    @Container
    val container = MariaDBContainer("mariadb")

    override fun getContainer(): JdbcDatabaseContainer<*> = container

    override fun getProperties(): SQLDataSourceProperties = SQLDataSourceProperties(
        "mariadb",
        container.host,
        container.firstMappedPort,
        container.username,
        container.password,
        container.databaseName
    )
}
