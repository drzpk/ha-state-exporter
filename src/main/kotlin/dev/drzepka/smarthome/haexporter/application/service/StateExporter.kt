package dev.drzepka.smarthome.haexporter.application.service

import dev.drzepka.smarthome.haexporter.application.converter.FlowConverter
import dev.drzepka.smarthome.haexporter.application.properties.ExporterProperties
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.service.EntityIdResolver
import dev.drzepka.smarthome.haexporter.domain.service.ProcessingStrategyResolver
import dev.drzepka.smarthome.haexporter.domain.value.EntityStateTime
import org.apache.logging.log4j.kotlin.Logging

class StateExporter(
    private val strategyResolver: ProcessingStrategyResolver,
    private val metadataProvider: HomeAssistantEntityMetadataProvider,
    private val entityIdResolver: EntityIdResolver,
    private val stateProvider: HomeAssistantStateProvider,
    private val statePipeline: StatePipeline,
    private val stateRepository: StateRepository,
    exporterProperties: ExporterProperties
) {
    private val flowConverter = FlowConverter(exporterProperties.batchSize, exporterProperties.processingLimit, stateProvider::getStates)

    suspend fun export() {
        try {
            doExport()
        } catch (e: Exception) {
            logger.error(e) { "Error while exporting states" }
        }
    }

    private suspend fun doExport() {
        logger.info { "Starting to export states" }

        val currentStates = getCurrentStates()
        val strategy = strategyResolver.resolve(currentStates)
        logger.info { "Detected ${currentStates.size} current states" }

        val input = flowConverter.execute(strategy.getWorkUnits())
        val output = statePipeline.execute(input)
        stateRepository.save(output)
    }

    private suspend fun getCurrentStates(): List<EntityStateTime> = metadataProvider
        .getEntityMetadata()
        .also { logger.debug { "Received entity metadata: $it" } }
        .map { entityIdResolver.resolve(it.entityId) to it }
        .mapNotNull { if (it.first != null) EntityStateTime(it.first!!, it.second.lastChanged) else null }

    companion object : Logging
}
