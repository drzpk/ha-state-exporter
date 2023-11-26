package dev.drzepka.smarthome.haexporter.domain.value.strategy

import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.domain.value.EntityStateLag
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class ChanneledStrategyTest {

    @Test
    fun `should order work units based on lag size`() {
        val time = Instant.now().truncatedTo(ChronoUnit.MINUTES)
        val lags = listOf(
            EntityStateLag(ENTITY_1, time + Duration.ofMinutes(11), time + Duration.ofMinutes(9)),
            EntityStateLag(ENTITY_2, time + Duration.ofMinutes(8), time + Duration.ofMinutes(8)),
            EntityStateLag(ENTITY_3, time + Duration.ofMinutes(15), time + Duration.ofMinutes(7)),
            EntityStateLag(ENTITY_4, time + Duration.ofMinutes(15), time + Duration.ofMinutes(7)),
            EntityStateLag(ENTITY_5, time + Duration.ofMinutes(34), null),
            EntityStateLag(ENTITY_6, time + Duration.ofMinutes(21), null)
        )

        val units = ChanneledStrategy(lags).getWorkUnits()

        then(units).containsExactly(
            WorkUnit(Instant.EPOCH, setOf(ENTITY_5)),
            WorkUnit(Instant.EPOCH, setOf(ENTITY_6)),
            WorkUnit(time + Duration.ofMinutes(7), setOf(ENTITY_3)),
            WorkUnit(time + Duration.ofMinutes(7), setOf(ENTITY_4)),
            WorkUnit(time + Duration.ofMinutes(9), setOf(ENTITY_1)),
            WorkUnit(time + Duration.ofMinutes(8), setOf(ENTITY_2))
        )
    }

    companion object {
        private val ENTITY_1 = EntityId("class", "device", "sensor-1")
        private val ENTITY_2 = EntityId("class", "device", "sensor-2")
        private val ENTITY_3 = EntityId("class", "device", "sensor-3")
        private val ENTITY_4 = EntityId("class", "device", "sensor-4")
        private val ENTITY_5 = EntityId("class", "device", "sensor-5")
        private val ENTITY_6 = EntityId("class", "device", "sensor-6")
    }
}
