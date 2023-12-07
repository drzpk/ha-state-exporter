package dev.drzepka.smarthome.haexporter.infrastructure.repository

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.util.chunked
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.infrastructure.database.InfluxDBClientProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.addField
import kotlinx.coroutines.flow.Flow
import org.apache.logging.log4j.kotlin.Logging
import java.time.Instant

class InfluxDBStateRepository(private val provider: InfluxDBClientProvider) : StateRepository {

    override suspend fun save(states: Flow<State>) {
        val stats = Stats()
        states.chunked(5000)
            .collect { chunk ->
                val points = chunk.map { it.toPoint() }
                provider.client
                    .getWriteKotlinApi()
                    .writePoints(points)

                logger.info { "Saved ${chunk.size} states, chunk #${stats.chunkCount}" }
                stats.update(chunk)
            }

        logger.info { stats.describe() }
    }

    override suspend fun getLastStateTime(entity: EntityId): Instant? {
        val flux = Flux
            .from(provider.bucket)
            .range(Instant.EPOCH, Instant.now())
            .filter(Restrictions.tag(CLASS_TAG).equal(entity.classValue))
            .filter(Restrictions.tag(DEVICE_TAG).equal(entity.device))
            .filter(Restrictions.field().equal(entity.getFieldName()))
            .keep(arrayOf(TIME_COLUMN))
            .last(TIME_COLUMN)

        return provider.client
            .getQueryKotlinApi()
            .query(flux.toString())
            .receiveCatching()
            .getOrNull()
            ?.time
    }

    private fun State.toPoint(): Point {
        return Point(measurement)
            .time(time, WritePrecision.S)
            .addField(entity.getFieldName(), value)
            .addTag(CLASS_TAG, entity.classValue)
            .addTag(DEVICE_TAG, entity.device)
            .addTag(SENSOR_TAG, entity.sensor)
    }

    private fun EntityId.getFieldName(): String = sensor ?: MISSING_MEASUREMENT_FIELD

    private class Stats {
        var earliestState: Instant? = null
        var latestState: Instant? = null
        var stateCount = 0
        var chunkCount = 0

        fun update(chunk: List<State>) {
            earliestState = minOf(earliestState ?: Instant.MAX, chunk.minOf { it.time })
            latestState = maxOf(latestState ?: Instant.MIN, chunk.maxOf { it.time })
            stateCount += chunk.size
            chunkCount++
        }

        fun describe(): String {
            return "Finished saving $stateCount states in $chunkCount chunks. " +
                "First state time: $earliestState, last state time: $latestState"
        }
    }

    companion object : Logging {
        private const val MISSING_MEASUREMENT_FIELD = "value"
        private const val CLASS_TAG = "class"
        private const val DEVICE_TAG = "device"
        private const val SENSOR_TAG = "sensor"
        private const val TIME_COLUMN = "_time"
    }
}
