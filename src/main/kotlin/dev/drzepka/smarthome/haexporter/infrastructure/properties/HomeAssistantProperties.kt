package dev.drzepka.smarthome.haexporter.infrastructure.properties

data class HomeAssistantProperties(
    val database: SQLDataSourceProperties,
    val api: HomeAssistantAPIProperties
)