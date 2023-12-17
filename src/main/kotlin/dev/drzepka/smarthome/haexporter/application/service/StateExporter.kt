package dev.drzepka.smarthome.haexporter.application.service

import dev.drzepka.smarthome.haexporter.application.converter.FlowConverter
import dev.drzepka.smarthome.haexporter.application.properties.ExporterProperties
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.domain.entity.ExportStatus
import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.repository.ExportStatusRepository
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.service.EntityIdResolver
import dev.drzepka.smarthome.haexporter.domain.service.ProcessingStrategyResolver
import dev.drzepka.smarthome.haexporter.domain.value.EntityStateTime
import kotlinx.coroutines.flow.onEach
import org.apache.logging.log4j.kotlin.Logging
import java.time.Clock
import java.time.Instant

class StateExporter(
    private val strategyResolver: ProcessingStrategyResolver,
    private val metadataProvider: HomeAssistantEntityMetadataProvider,
    private val entityIdResolver: EntityIdResolver,
    private val stateProvider: HomeAssistantStateProvider,
    private val statePipeline: StatePipeline,
    private val stateRepository: StateRepository,
    private val exportStatusRepository: ExportStatusRepository,
    private val clock: Clock,
    exporterProperties: ExporterProperties
) {
    private val flowConverter = FlowConverter(exporterProperties.batchSize, exporterProperties.processingLimit, stateProvider::getStates)

    suspend fun export() {
        val tracker = ExportTracker()
        try {
            doExport(tracker)
        } catch (e: Exception) {
            logger.error(e) { "Error while exporting states" }
            tracker.exception = e
        }

        saveExportStatus(tracker)
    }

    private suspend fun doExport(tracker: ExportTracker) {
        logger.info { "Starting to export states" }

        val currentStates = getCurrentStates()
        val strategy = strategyResolver.resolve(currentStates)
        tracker.entities = currentStates.size
        logger.info { "Detected ${currentStates.size} current states" }

        val input = flowConverter
            .execute(strategy.getWorkUnits())
            .onEach { tracker.onSourceState() }

        val output = statePipeline
            .execute(input)
            .onEach(tracker::onState)

        stateRepository.save(output)
        logger.info { "Finished exporting states" }
    }

    private suspend fun getCurrentStates(): List<EntityStateTime> = metadataProvider
        .getEntityMetadata()
        .also { logger.debug { "Received entity metadata: $it" } }
        .map { entityIdResolver.resolve(it.entityId) to it }
        .mapNotNull { if (it.first != null) EntityStateTime(it.first!!, it.second.lastChanged) else null }

    private suspend fun saveExportStatus(tracker: ExportTracker) {
        try {
            val status = tracker.toExportStatus()
            exportStatusRepository.save(status)
        } catch (e: Exception) {
            logger.error(e) { "Error while saving export status: $tracker" }
        }
    }

    private inner class ExportTracker {
        var entities: Int = 0
        var exception: Exception? = null

        private val startTime: Instant = clock.instant()
        private var loadedStates = 0
        private var savedStates = 0

        private var firstStateTime: Instant? = null
        private var lastStateTime: Instant? = null

        fun onSourceState() {
            loadedStates++
        }

        fun onState(state: State) {
            savedStates++
            firstStateTime = minOf(firstStateTime ?: Instant.MAX, state.time)
            lastStateTime = maxOf(lastStateTime ?: Instant.MIN, state.time)
        }

        fun toExportStatus(): ExportStatus {
            val finishTime = clock.instant()
            return ExportStatus(
                time = finishTime,
                startedAt = startTime,
                finishedAt = finishTime,
                success = exception == null,
                exception = exception?.let { it.javaClass.canonicalName + ": " + it.message },
                loadedStates = loadedStates,
                savedStates = savedStates,
                entities = entities,
                firstStateTime = firstStateTime,
                lastStateTime = lastStateTime
            )
        }

        override fun toString(): String = "ExportTracker(status=${toExportStatus()})"
    }

    companion object : Logging
}
