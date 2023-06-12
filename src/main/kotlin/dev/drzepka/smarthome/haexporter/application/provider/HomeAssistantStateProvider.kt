package dev.drzepka.smarthome.haexporter.application.provider

import dev.drzepka.smarthome.haexporter.application.model.SourceState
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface HomeAssistantStateProvider {
    suspend fun getStates(fromInclusive: Instant, limit: Int): Flow<SourceState>
}
