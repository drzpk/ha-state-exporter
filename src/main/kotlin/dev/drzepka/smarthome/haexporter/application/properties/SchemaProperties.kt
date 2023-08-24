package dev.drzepka.smarthome.haexporter.application.properties

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
    val type: ValueType = ValueType.AUTO,
    val stateMapping: String? = null,
    val ignoredValues: List<String> = emptyList()
) {
    companion object {
        val DEFAULT = EntitySchema(ANY_SENSOR, type = ValueType.AUTO, null, emptyList())
    }
}

const val ANY_SENSOR = "*"

enum class ValueType {
    NUMBER, STRING, AUTO
}
