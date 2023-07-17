package dev.drzepka.smarthome.haexporter.infrastructure.repository

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.infrastructure.database.InfluxDBClientProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.addField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class InfluxDBStateRepository(private val provider: InfluxDBClientProvider) : StateRepository {

    override suspend fun save(states: Flow<State>) {
        val mappedStates = states.map { it.toPoint() }
        provider.client
            .getWriteKotlinApi()
            .writePoints(mappedStates)
    }

    override suspend fun getLastStateTime(entity: EntityId): Instant? {
        val flux = Flux
            .from(provider.bucket)
            .range(Instant.EPOCH, Instant.now())
            .filter(Restrictions.measurement().equal(entity.classValue))
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
        return Point(entity.classValue)
            .time(time, WritePrecision.S)
            .addField(entity.getFieldName(), value)
            .addTag(DEVICE_TAG, entity.device)
    }

    private fun EntityId.getFieldName(): String = sensor ?: MISSING_MEASUREMENT_FIELD

    companion object {
        private const val MISSING_MEASUREMENT_FIELD = "value"
        private const val DEVICE_TAG = "device"
        private const val TIME_COLUMN = "_time"
    }
}
