package dev.drzepka.smarthome.haexporter.domain.repository

import dev.drzepka.smarthome.haexporter.domain.entity.SourceState
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SourceStateRepository {
    suspend fun getStates(fromInclusive: Instant, limit: Int): Flow<SourceState>
}
