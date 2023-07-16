package dev.drzepka.smarthome.haexporter.domain.value

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

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

    companion object {
        @JvmStatic
        private fun getMatchedEntitySelectors(): Stream<Pair<EntityId, EntitySelector>> = listOf(
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door", "state"),
            EntityId("sensor", "main_door", "state") to EntitySelector(ANY_VALUE, "main_door", "state"),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door", ANY_VALUE),
            EntityId("sensor", "main_door", "state") to EntitySelector(ANY_VALUE, "main_door", ANY_VALUE),

            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor", "sensor1"), "main_door", listOf("state")),
            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor"), "main_door", listOf("state", "state1")),
            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor", "sensor1"), "main_door", listOf("state", "state1")),

            EntityId("sensor", "main_door", "state") to EntitySelector(listOf(ANY_VALUE), "main_door", listOf("state", "state1")),
            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor", "sensor1"), "main_door", listOf(ANY_VALUE)),

            EntityId("sensor", "main_door", null) to EntitySelector(listOf("sensor", "sensor1"), "main_door", null)
        ).stream()

        @JvmStatic
        private fun getUnmatchedEntitySelectors(): Stream<Pair<EntityId, EntitySelector>> = listOf(
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door", "state1"),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door1", "state"),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor1", "main_door", "state"),

            EntityId("sensor", "main_door", "state") to EntitySelector(ANY_VALUE, "main_door", "state1"),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor1", "main_door", ANY_VALUE),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", ANY_VALUE, "state"),

            EntityId("sensor", "main_door", "state") to EntitySelector(listOf("sensor1"), "main_door", listOf("state1")),
            EntityId("sensor", "main_door", "state") to EntitySelector("sensor", "main_door", null),
            EntityId("sensor", "main_door", null) to EntitySelector("sensor", "main_door", "state"),
        ).stream()
    }
}
