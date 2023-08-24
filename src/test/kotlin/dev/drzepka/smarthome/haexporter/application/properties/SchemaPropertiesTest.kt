package dev.drzepka.smarthome.haexporter.application.properties

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

internal class SchemaPropertiesTest {

    @Test
    fun `should get correct entity schema`() {
        val properties = SchemaProperties(
            "name",
            "influxName",
            listOf(
                EntitySchema(sensor = "phase_a", stateMapping = "mapping-a"),
                EntitySchema(sensor = null, stateMapping = "mapping-b"),
                EntitySchema(sensor = "*", stateMapping = "mapping-c")
            )
        )

        then(properties.getEntitySchema("phase_a")).matches { it.stateMapping == "mapping-a" }
        then(properties.getEntitySchema(null)).matches { it.stateMapping == "mapping-b" }
        then(properties.getEntitySchema("phase_c")).matches { it.stateMapping == "mapping-c" }
    }

    @Test
    fun `should get built-in default entity schema if one isn't present in properties`() {
        val properties = SchemaProperties(
            "name",
            "influxName",
            listOf(
                EntitySchema(sensor = "phase_a", stateMapping = "mapping-a")
            )
        )

        then(properties.getEntitySchema("test")).isSameAs(EntitySchema.DEFAULT)
    }
}
