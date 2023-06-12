package dev.drzepka.smarthome.haexporter.domain.repository

import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface StateRepository {
    suspend fun save(states: Flow<State>)
    suspend fun getLastStateTime(entity: EntityId): Instant?
}
