package dev.drzepka.smarthome.haexporter.application.properties

import com.fasterxml.jackson.annotation.JsonProperty
import dev.drzepka.smarthome.haexporter.domain.properties.EntityProperties
import dev.drzepka.smarthome.haexporter.domain.value.StateMapping
import dev.drzepka.smarthome.haexporter.infrastructure.properties.InfluxDBDataSourceProperties
import dev.drzepka.smarthome.haexporter.infrastructure.properties.SQLDataSourceProperties

data class RootProperties( // todo: split into separate beans
    val homeAssistant: SQLDataSourceProperties,
    @field:JsonProperty("influxdb")
    val influxDB: InfluxDBDataSourceProperties,
    val exporter: ExporterProperties = ExporterProperties(),
    val entities: List<EntityProperties> = emptyList(),
    val schemas: List<SchemaProperties> = emptyList(),
    val stateMappings: List<StateMapping> = emptyList()
)
