package dev.drzepka.smarthome.haexporter.infrastructure.repository

import dev.drzepka.smarthome.haexporter.infrasctucture.properties.SQLDataSourceProperties
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container

class MariaDBSourceStateRepositoryTest : BaseSQLSourceStateRepositoryTest() {
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
