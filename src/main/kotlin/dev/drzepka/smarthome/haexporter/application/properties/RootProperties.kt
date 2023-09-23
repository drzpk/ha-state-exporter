package dev.drzepka.smarthome.haexporter.application.properties

import com.fasterxml.jackson.annotation.JsonProperty
import dev.drzepka.smarthome.haexporter.domain.properties.EntityProperties
import dev.drzepka.smarthome.haexporter.domain.value.StateMapping
import dev.drzepka.smarthome.haexporter.infrastructure.properties.HomeAssistantProperties
import dev.drzepka.smarthome.haexporter.infrastructure.properties.InfluxDBDataSourceProperties

data class RootProperties(
    @field:JsonProperty("homeassistant")
    val homeAssistant: HomeAssistantProperties,
    @field:JsonProperty("influxdb")
    val influxDB: InfluxDBDataSourceProperties,
    val exporter: ExporterProperties = ExporterProperties(),
    val entities: List<EntityProperties> = emptyList(),
    val schemas: List<SchemaProperties> = emptyList(),
    val stateMappings: List<StateMapping> = emptyList()
)
