package dev.drzepka.smarthome.haexporter.infrastructure.repository

import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.util.trimToSeconds
import dev.drzepka.smarthome.haexporter.domain.value.*
import dev.drzepka.smarthome.haexporter.infrastructure.database.InfluxDBClientProvider
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
            sensorState(time.plusSeconds(1), "computer", "temp1", "temperature", NumericStateValue(23)),
            sensorState(time.plusSeconds(2), "computer", "temp2", "temperature", NumericStateValue(42.3)),
            sensorState(time.plusSeconds(3), "computer", "state", "temperature", StringStateValue("ok")),
            sensorState(time.plusSeconds(4), "thermometer", null, "temperature", NumericStateValue(12)),
            sensorState(time.plusSeconds(5), "thermometer", "enabled", "th_state", BooleanStateValue(true)),
        )

        repository.save(states.asFlow())

        val records = getRecords()
        then(records).hasSize(5)

        val trimmedTime = time.trimToSeconds()
        records.assertContains(trimmedTime.plusSeconds(1), "temperature", "temp1", 23L, tags("computer", "temp1"))
        records.assertContains(trimmedTime.plusSeconds(2), "temperature", "temp2", 42.3, tags("computer", "temp2"))
        records.assertContains(trimmedTime.plusSeconds(3), "temperature", "state", "ok", tags("computer", "state"))
        records.assertContains(trimmedTime.plusSeconds(4), "temperature", "value", 12L, tags("thermometer", null))
        records.assertContains(trimmedTime.plusSeconds(5), "th_state", "enabled", true, tags("thermometer", "enabled"))
    }

    @Test
    fun `should get time of last existing state`() = runBlocking {
        val time = Instant.now().minusSeconds(10).trimToSeconds()
        val states = listOf(
            sensorState(time.plusSeconds(1), "wind", "station_1", "wind", NumericStateValue(12)),
            sensorState(time.plusSeconds(2), "wind", "station_1", "wind", NumericStateValue(23)),
            sensorState(time.plusSeconds(3), "wind", "station_2", "wind", NumericStateValue(34)),
            //////
            sensorState(time.plusSeconds(4), "wind", null, "wind", NumericStateValue(45)),
            sensorState(time.plusSeconds(5), "wind", null, "wind", NumericStateValue(56))
        )

        repository.save(states.asFlow())
        delay(3000)

        then(repository.getLastStateTime(entityId("wind", "station_1"))).isEqualTo(time.plusSeconds(2))
        then(repository.getLastStateTime(entityId("wind", "station_2"))).isEqualTo(time.plusSeconds(3))
        then(repository.getLastStateTime(entityId("wind", null))).isEqualTo(time.plusSeconds(5))
        then(repository.getLastStateTime(entityId("unknown", "test"))).isNull()
        then(repository.getLastStateTime(entityId("unknown", null))).isNull()
    }


    private fun sensorState(time: Instant, device: String, sensor: String?, measurement: String, value: StateValue): State =
        State(time, entityId(device, sensor), measurement, value)

    private fun entityId(device: String, sensor: String?): EntityId = EntityId(CLAZZ, device, sensor)

    private fun tags(dev: String, sensor: String?): Map<String, String> =
        mutableMapOf("class" to CLAZZ, "device" to dev).also {
            if (sensor != null)
                it["sensor"] = sensor
        }

    companion object {
        private const val CLAZZ = "sensor"
    }
}
