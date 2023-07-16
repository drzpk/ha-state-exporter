package dev.drzepka.smarthome.haexporter.application.provider

import dev.drzepka.smarthome.haexporter.application.model.SourceState
import java.time.Instant

interface HomeAssistantStateProvider {
    suspend fun getStates(fromInclusive: Instant, offset: Int, limit: Int): List<SourceState>
}
