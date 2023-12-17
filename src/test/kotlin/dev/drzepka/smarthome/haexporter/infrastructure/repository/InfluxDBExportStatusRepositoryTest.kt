package dev.drzepka.smarthome.haexporter.infrastructure.repository

import dev.drzepka.smarthome.haexporter.domain.entity.ExportStatus
import dev.drzepka.smarthome.haexporter.domain.util.trimToSeconds
import dev.drzepka.smarthome.haexporter.infrastructure.database.InfluxDBClientProvider
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Instant

class InfluxDBExportStatusRepositoryTest : BaseInfluxDBTest() {

    private val repository by lazy {
        InfluxDBExportStatusRepository(InfluxDBClientProvider(getDataSourceProperties()))
    }

    @Test
    fun `should save export status`() = runBlocking {
        val referenceTime = Instant.now().trimToSeconds()
        val status = ExportStatus(
            referenceTime,
            referenceTime.plusSeconds(1),
            referenceTime.plusSeconds(2),
            false,
            "Exception message",
            12,
            34,
            56,
            referenceTime.plusSeconds(3),
            referenceTime.plusSeconds(4)
        )

        repository.save(status)

        val records = getRecords()
        then(records).hasSize(8)

        val measurement = "ha_state_exporter_status"
        val tags = mapOf("status" to "failure")
        records.assertContains(referenceTime, measurement, "started_at", status.startedAt.toEpochMilli(), tags)
        records.assertContains(referenceTime, measurement, "finished_at", status.finishedAt.toEpochMilli(), tags)
        records.assertContains(referenceTime, measurement, "exception", status.exception!!, tags)
        records.assertContains(referenceTime, measurement, "loaded_states", status.loadedStates.toLong(), tags)
        records.assertContains(referenceTime, measurement, "saved_states", status.savedStates.toLong(), tags)
        records.assertContains(referenceTime, measurement, "entities", status.entities.toLong(), tags)
        records.assertContains(referenceTime, measurement, "first_state_time", status.firstStateTime!!.toEpochMilli(), tags)
        records.assertContains(referenceTime, measurement, "last_state_time", status.lastStateTime!!.toEpochMilli(), tags)
    }
}
