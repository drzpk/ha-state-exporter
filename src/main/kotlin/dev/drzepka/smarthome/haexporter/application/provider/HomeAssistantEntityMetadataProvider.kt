package dev.drzepka.smarthome.haexporter.application.provider

import dev.drzepka.smarthome.haexporter.application.model.EntityMetadata

interface HomeAssistantEntityMetadataProvider {
    suspend fun getEntityMetadata(): List<EntityMetadata>
}
