package dev.drzepka.smarthome.haexporter.application.provider

import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.model.SourceStateQuery

interface HomeAssistantStateProvider {
    suspend fun getStates(query: SourceStateQuery): List<SourceState>
}
