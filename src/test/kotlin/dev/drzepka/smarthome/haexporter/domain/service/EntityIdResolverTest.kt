package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.properties.DeviceProperties
import dev.drzepka.smarthome.haexporter.domain.properties.DevicesProperties
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EntityIdResolverTest {

    private val properties = DevicesProperties().apply {
        add(DeviceProperties("living_room", "schema"))
        add(DeviceProperties("kitchen_light", "schema"))
    }

    private val resolver = EntityIdResolver(properties)

    @ParameterizedTest
    @ValueSource(strings = ["l1", "l1_l3"])
    fun `should resolve entity id with suffix`(suffix: String) {
        val resolved = resolver.resolve("sensor.living_room_$suffix")

        then(resolved).isNotNull
        resolved as EntityId

        then(resolved.domainValue).isEqualTo("sensor")
        then(resolved.device).isEqualTo("living_room")
        then(resolved.suffix).isEqualTo(suffix)
    }

    @Test
    fun `should resolve entity id without suffix`() {
        val resolved = resolver.resolve("sensor.kitchen_light")

        then(resolved).isNotNull
        resolved as EntityId

        then(resolved.domainValue).isEqualTo("sensor")
        then(resolved.device).isEqualTo("kitchen_light")
        then(resolved.suffix).isNull()
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
