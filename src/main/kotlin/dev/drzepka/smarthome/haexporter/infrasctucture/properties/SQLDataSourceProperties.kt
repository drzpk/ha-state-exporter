package dev.drzepka.smarthome.haexporter.infrasctucture.properties

data class SQLDataSourceProperties(
    val driver: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val database: String
)
