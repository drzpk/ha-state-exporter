package dev.drzepka.smarthome.haexporter.application.properties

import dev.drzepka.smarthome.haexporter.domain.value.ValueType

class SchemasProperties(private val properties: List<SchemaProperties>) : List<SchemaProperties> by properties

data class SchemaProperties(
    val name: String,
    val influxMeasurementName: String,
    val entities: List<EntitySchema> = emptyList()
) {

    fun getEntitySchema(sensor: String?): EntitySchema {
        var result = entities.firstOrNull { it.sensor == sensor }
        if (result == null)
            result = entities.firstOrNull { it.sensor == ANY_SENSOR }
        if (result == null)
            result = EntitySchema.DEFAULT

        return result
    }
}

data class EntitySchema(
    val sensor: String? = null,
    val type: ValueType = ValueType.STRING,
    val stateMapping: String? = null,
    val ignoreUnmappedState: Boolean = true,
    val ignoredValues: List<String> = emptyList(), // todo: implement this feature
) {
    companion object {
        val DEFAULT = EntitySchema(sensor = ANY_SENSOR)
    }
}

const val ANY_SENSOR = "*"
