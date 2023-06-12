package dev.drzepka.smarthome.haexporter.infrastructure.repository

import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.util.trimToSeconds
import dev.drzepka.smarthome.haexporter.domain.value.*
import dev.drzepka.smarthome.haexporter.infrasctucture.database.InfluxDBClientProvider
import dev.drzepka.smarthome.haexporter.infrasctucture.repository.InfluxDBStateRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Instant

class InfluxDBStateRepositoryTest : BaseInfluxDBTest() {

    private val repository by lazy {
        InfluxDBStateRepository(InfluxDBClientProvider(getDataSourceProperties()))
    }

    @Test
    fun `should save states`() = runBlocking {
        val time = Instant.now()
        val states = listOf(
            sensorState(time.plusSeconds(1), "computer", "temp1", NumericStateValue(23)),
            sensorState(time.plusSeconds(2), "computer", "temp2", NumericStateValue(42.3)),
            sensorState(time.plusSeconds(3), "computer", "state", StringStateValue("ok")),
            sensorState(time.plusSeconds(4), "thermometer", null, NumericStateValue(12)),
            sensorState(time.plusSeconds(5), "thermometer", "enabled", BooleanStateValue(true)),
        )

        repository.save(states.asFlow())

        val records = getRecords()
        then(records).hasSize(5)

        val trimmedTime = time.trimToSeconds()
        records.assertContains(trimmedTime.plusSeconds(1), DOMAIN, "temp1", 23L, mapOf("device" to "computer"))
        records.assertContains(trimmedTime.plusSeconds(2), DOMAIN, "temp2", 42.3, mapOf("device" to "computer"))
        records.assertContains(trimmedTime.plusSeconds(3), DOMAIN, "state", "ok", mapOf("device" to "computer"))
        records.assertContains(trimmedTime.plusSeconds(4), DOMAIN, "value", 12L, mapOf("device" to "thermometer"))
        records.assertContains(trimmedTime.plusSeconds(5), DOMAIN, "enabled", true, mapOf("device" to "thermometer"))
    }

    @Test
    fun `should get time of last existing state`() = runBlocking {
        val time = Instant.now().minusSeconds(10).trimToSeconds()
        val states = listOf(
            sensorState(time.plusSeconds(1), "wind", "station_1", NumericStateValue(12)),
            sensorState(time.plusSeconds(2), "wind", "station_1", NumericStateValue(23)),
            sensorState(time.plusSeconds(3), "wind", "station_2", NumericStateValue(34)),
            //////
            sensorState(time.plusSeconds(4), "wind", null, NumericStateValue(45)),
            sensorState(time.plusSeconds(5), "wind", null, NumericStateValue(56))
        )

        repository.save(states.asFlow())
        delay(3000)

        then(repository.getLastStateTime(entityId("wind", "station_1"))).isEqualTo(time.plusSeconds(2))
        then(repository.getLastStateTime(entityId("wind", "station_2"))).isEqualTo(time.plusSeconds(3))
        then(repository.getLastStateTime(entityId("wind", null))).isEqualTo(time.plusSeconds(5))
        then(repository.getLastStateTime(entityId("unknown", "test"))).isNull()
        then(repository.getLastStateTime(entityId("unknown", null))).isNull()
    }


    private fun sensorState(time: Instant, device: String, suffix: String?, value: StateValue): State =
        State(time, entityId(device, suffix), value)

    private fun entityId(device: String, suffix: String?): EntityId = EntityId(DOMAIN, device, suffix)

    companion object {
        private const val DOMAIN = "sensor"
    }
}
