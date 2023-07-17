package dev.drzepka.smarthome.haexporter.application.model

import dev.drzepka.smarthome.haexporter.domain.value.ElementalEntitySelector
import dev.drzepka.smarthome.haexporter.domain.value.EntitySelector
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

internal class EntitySelectorTest {

    @Test
    fun `should split single entity selector to elemental selector`() {
        val selector = EntitySelector("class", "device", "sensor")
        val split = selector.toElementalSelectors()

        then(split).hasSize(1)
        then(split.first()).isEqualTo(ElementalEntitySelector("class", "device", "sensor"))
    }

    @Test
    fun `should split compound entity selector to elemental selectors`() {
        val selector = EntitySelector(listOf("class1", "class2"), "device", listOf("sensor1", "sensor2", "sensor3"))
        val split = selector.toElementalSelectors()

        then(split).hasSize(6)
        then(split[0]).isEqualTo(ElementalEntitySelector("class1", "device", "sensor1"))
        then(split[1]).isEqualTo(ElementalEntitySelector("class1", "device", "sensor2"))
        then(split[2]).isEqualTo(ElementalEntitySelector("class1", "device", "sensor3"))
        then(split[3]).isEqualTo(ElementalEntitySelector("class2", "device", "sensor1"))
        then(split[4]).isEqualTo(ElementalEntitySelector("class2", "device", "sensor2"))
        then(split[5]).isEqualTo(ElementalEntitySelector("class2", "device", "sensor3"))
    }
}
