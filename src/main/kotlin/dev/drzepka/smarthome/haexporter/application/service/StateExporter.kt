package dev.drzepka.smarthome.haexporter.application.service

import dev.drzepka.smarthome.haexporter.application.converter.FlowConverter
import dev.drzepka.smarthome.haexporter.application.model.EntityIdWrapper
import dev.drzepka.smarthome.haexporter.application.model.EntityMetadata
import dev.drzepka.smarthome.haexporter.application.properties.ExporterProperties
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.service.EntityIdResolver
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import org.apache.logging.log4j.kotlin.Logging
import java.time.Instant

class StateExporter(
    private val stateRepository: StateRepository,
    private val metadataProvider: HomeAssistantEntityMetadataProvider,
    private val exporterProperties: ExporterProperties,
    private val entityIdResolver: EntityIdResolver,
    private val stateProvider: HomeAssistantStateProvider,
    private val statePipeline: StatePipeline
) {
    private val flowConverter = FlowConverter(exporterProperties.batchSize, stateProvider::getStates)

    suspend fun export() {
        try {
            doExport()
        } catch (e: Exception) {
            logger.error(e) { "Error while exporting state" }
        }
    }

    private suspend fun doExport() {
        logger.info { "Starting to export states" }

        val metadata = getMetadata()
        val startTime = getMostRecentStateTime(metadata.map { it.entityId })

        val flow = flowConverter.execute(startTime, exporterProperties.processingLimit)
        statePipeline.execute(flow)
    }

    private suspend fun getMetadata(): List<EntityIdWrapper<EntityMetadata>> = metadataProvider
        .getEntityMetadata()
        .map { entityIdResolver.resolve(it.entityId) to it }
        .mapNotNull { if (it.first != null) EntityIdWrapper(it.first!!, it.second) else null }
        .also { logger.debug { "Received entity metadata: $it" } }

    private suspend fun getMostRecentStateTime(metadata: List<EntityId>): Instant {
        val lastTime = metadata
            .mapNotNull { stateRepository.getLastStateTime(it) }
            .maxOrNull()

        return if (lastTime != null) {
            logger.info { "Most recent state time is $lastTime" }
            lastTime
        } else {
            logger.info { "No states were found with matching entity IDs. State export will start from beginning " }
            Instant.EPOCH
        }
    }

    companion object : Logging
}
