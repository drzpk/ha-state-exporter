package dev.drzepka.smarthome.haexporter.application.model

import dev.drzepka.smarthome.haexporter.domain.value.ElementalEntitySelector
import dev.drzepka.smarthome.haexporter.domain.value.EntitySelector
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

internal class EntitySelectorTest {

    @Test
    fun `should split single entity selector to elemental selector`() {
        val selector = EntitySelector("domain", "device", "suffix")
        val split = selector.toElementalSelectors()

        then(split).hasSize(1)
        then(split.first()).isEqualTo(ElementalEntitySelector("domain", "device", "suffix"))
    }

    @Test
    fun `should split compound entity selector to elemental selectors`() {
        val selector = EntitySelector(listOf("domain1", "domain2"), "device", listOf("suffix1", "suffix2", "suffix3"))
        val split = selector.toElementalSelectors()

        then(split).hasSize(6)
        then(split[0]).isEqualTo(ElementalEntitySelector("domain1", "device", "suffix1"))
        then(split[1]).isEqualTo(ElementalEntitySelector("domain1", "device", "suffix2"))
        then(split[2]).isEqualTo(ElementalEntitySelector("domain1", "device", "suffix3"))
        then(split[3]).isEqualTo(ElementalEntitySelector("domain2", "device", "suffix1"))
        then(split[4]).isEqualTo(ElementalEntitySelector("domain2", "device", "suffix2"))
        then(split[5]).isEqualTo(ElementalEntitySelector("domain2", "device", "suffix3"))
    }
}
