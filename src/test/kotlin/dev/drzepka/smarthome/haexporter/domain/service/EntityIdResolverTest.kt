package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.value.EntitySelector
import dev.drzepka.smarthome.haexporter.domain.properties.EntitiesProperties
import dev.drzepka.smarthome.haexporter.domain.properties.EntityProperties
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EntityIdResolverTest {

    private val properties = EntitiesProperties().apply {
        add(EntityProperties(EntitySelector(device = "living_room"), "mapping1"))
        add(EntityProperties(EntitySelector(device = "kitchen_light"), "mapping2"))
    }

    private val resolver = EntityIdResolver(properties)

    @ParameterizedTest
    @ValueSource(strings = ["l1", "l1_l3"])
    fun `should resolve entity id with sensor`(sensor: String) {
        val resolved = resolver.resolve("sensor.living_room_$sensor")

        then(resolved).isNotNull
        resolved as EntityId

        then(resolved.classValue).isEqualTo("sensor")
        then(resolved.device).isEqualTo("living_room")
        then(resolved.sensor).isEqualTo(sensor)
    }

    @Test
    fun `should resolve entity id without sensor`() {
        val resolved = resolver.resolve("sensor.kitchen_light")

        then(resolved).isNotNull
        resolved as EntityId

        then(resolved.classValue).isEqualTo("sensor")
        then(resolved.device).isEqualTo("kitchen_light")
        then(resolved.sensor).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["something_else", "living_room1"])
    fun `should not resolve entity id if device is not configured`(input: String) {
        val resolved = resolver.resolve("sensor.$input")
        then(resolved).isNull()
    }

    @Test
    fun `should not resolve if value is malformed`() {
        val resolved = resolver.resolve("abcdef")
        then(resolved).isNull()
    }
}
