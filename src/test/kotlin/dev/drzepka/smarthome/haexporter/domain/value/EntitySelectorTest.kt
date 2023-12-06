package dev.drzepka.smarthome.haexporter.domain.value

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class EntitySelectorTest {

    @ParameterizedTest
    @MethodSource("getMatchedEntitySelectors")
    fun `should match entity selectors`(pair: Pair<EntityId, EntitySelector>) {
        val (entityId, selector) = pair
        then(selector.matches(entityId)).isTrue
    }

    @ParameterizedTest
    @MethodSource("getUnmatchedEntitySelectors")
    fun `should not match entity selectors`(pair: Pair<EntityId, EntitySelector>) {
        val (entityId, selector) = pair
        then(selector.matches(entityId)).isFalse
    }

    @Test
    fun `should split compound entity selector to elemental selectors`() {
        val selector = EntitySelector(
            listOf("class1", "class2"),
            listOf("device1", "device2"),
            listOf("sensor1", "sensor2", "sensor3")
        )

        val split = selector.toElementalSelectors()

        then(split).hasSize(12)
        then(split).containsExactlyInAnyOrder(
            ElementalEntitySelector("class1", "device1", "sensor1"),
            ElementalEntitySelector("class1", "device2", "sensor1"),
            ElementalEntitySelector("class1", "device1", "sensor2"),
            ElementalEntitySelector("class1", "device2", "sensor2"),
            ElementalEntitySelector("class1", "device1", "sensor3"),
            ElementalEntitySelector("class1", "device2", "sensor3"),
            ElementalEntitySelector("class2", "device1", "sensor1"),
            ElementalEntitySelector("class2", "device2", "sensor1"),
            ElementalEntitySelector("class2", "device1", "sensor2"),
            ElementalEntitySelector("class2", "device2", "sensor2"),
            ElementalEntitySelector("class2", "device1", "sensor3"),
            ElementalEntitySelector("class2", "device2", "sensor3"),
        )
    }

    companion object {
        @JvmStatic
        private fun getMatchedEntitySelectors(): List<Pair<EntityId, EntitySelector>> = listOf(
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door", "state"),
            EntityId("sensor", "main_door", "state") to EntitySelector(ANY_VALUE, "main_door", "state"),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door", ANY_VALUE),
            EntityId("sensor", "main_door", "state") to EntitySelector(ANY_VALUE, "main_door", ANY_VALUE),

            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor", "sensor1"), listOf("main_door"), listOf("state")),
            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor"), listOf("main_door"), listOf("state", "state1")),
            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor", "sensor1"), listOf("main_door"), listOf("state", "state1")),
            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor", "sensor1"), listOf("another_door", "main_door"), listOf("state", "state1")),

            EntityId("sensor", "main_door", "state") to EntitySelector(listOf(ANY_VALUE), listOf("main_door"), listOf("state", "state1")),
            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor", "sensor1"), listOf("main_door"), listOf(ANY_VALUE)),

            EntityId("sensor", "main_door", null) to EntitySelector(listOf("sensor", "sensor1"), listOf("main_door"), null)
        )

        @JvmStatic
        private fun getUnmatchedEntitySelectors(): List<Pair<EntityId, EntitySelector>> = listOf(
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door", "state1"),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door1", "state"),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor1", "main_door", "state"),

            EntityId("sensor", "main_door", "state") to EntitySelector(ANY_VALUE, "main_door", "state1"),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door1", ANY_VALUE),

            EntityId("sensor", "main_door", "state") to EntitySelector("sensor1", "main_door", "state1"),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door", null),
            EntityId("sensor", "main_door", null) to EntitySelector("sensor", "main_door", "state"),
        )
    }

    @Test
    fun `should split single entity selector to elemental selector`() {
        val selector = EntitySelector("class", "device", "sensor")
        val split = selector.toElementalSelectors()

        then(split).hasSize(1)
        then(split.first()).isEqualTo(ElementalEntitySelector("class", "device", "sensor"))
    }
}
