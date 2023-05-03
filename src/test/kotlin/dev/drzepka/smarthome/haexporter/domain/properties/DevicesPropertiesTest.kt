package dev.drzepka.smarthome.haexporter.domain.properties

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class DevicesPropertiesTest {

    @Test
    fun `should find properties by id`() {
        val device = DeviceProperties("dev_1", "a_type")
        val properties = DevicesProperties().apply { add(device) }

        then(properties.findById("dev_1")).isSameAs(device)
        then(properties.findById("non-existent")).isNull()
    }
}
