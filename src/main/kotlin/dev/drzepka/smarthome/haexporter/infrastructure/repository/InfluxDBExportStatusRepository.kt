package dev.drzepka.smarthome.haexporter.infrastructure.repository

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import dev.drzepka.smarthome.haexporter.domain.entity.ExportStatus
import dev.drzepka.smarthome.haexporter.domain.repository.ExportStatusRepository
import dev.drzepka.smarthome.haexporter.infrastructure.database.InfluxDBClientProvider
import org.apache.logging.log4j.kotlin.Logging

class InfluxDBExportStatusRepository(private val provider: InfluxDBClientProvider): ExportStatusRepository {

    override suspend fun save(status: ExportStatus) {
        if (provider.exportStatusBucket == null) {
            logger.trace("Export status bucket is not set, skipping saving export status")
            return
        }

        provider.client
            .getWriteKotlinApi()
            .writePoint(status.toPoint(), provider.exportStatusBucket)
    }

    private fun ExportStatus.toPoint(): Point = Point(MEASUREMENT_NAME)
        .time(time, WritePrecision.S)
        .addTag(TAG_STATUS, if (success) TAG_STATUS_SUCCESS else TAG_STATUS_FAILURE)
        .addField(FIELD_STARTED_AT, startedAt.toEpochMilli())
        .addField(FIELD_FINISHED_AT, finishedAt.toEpochMilli())
        .addField(FIELD_EXCEPTION, exception)
        .addField(FIELD_LOADED_STATES, loadedStates)
        .addField(FIELD_SAVED_STATES, savedStates)
        .addField(FIELD_ENTITIES, entities)
        .addField(FIELD_FIRST_STATE_TIME, firstStateTime?.toEpochMilli())
        .addField(FIELD_LAST_STATE_TIME, lastStateTime?.toEpochMilli())


    companion object : Logging {
        private const val MEASUREMENT_NAME = "ha_state_exporter_status"

        private const val TAG_STATUS = "status"
        private const val TAG_STATUS_SUCCESS = "success"
        private const val TAG_STATUS_FAILURE = "failure"

        private const val FIELD_STARTED_AT = "started_at"
        private const val FIELD_FINISHED_AT = "finished_at"
        private const val FIELD_EXCEPTION = "exception"
        private const val FIELD_LOADED_STATES = "loaded_states"
        private const val FIELD_SAVED_STATES = "saved_states"
        private const val FIELD_ENTITIES = "entities"
        private const val FIELD_FIRST_STATE_TIME = "first_state_time"
        private const val FIELD_LAST_STATE_TIME = "last_state_time"
    }
}
