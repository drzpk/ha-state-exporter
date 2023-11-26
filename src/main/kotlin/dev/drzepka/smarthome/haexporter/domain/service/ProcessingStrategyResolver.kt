package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.properties.ProcessingProperties
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.value.EntityStateLag
import dev.drzepka.smarthome.haexporter.domain.value.EntityStateTime
import dev.drzepka.smarthome.haexporter.domain.value.strategy.ChanneledStrategy
import dev.drzepka.smarthome.haexporter.domain.value.strategy.ProcessingStrategy
import dev.drzepka.smarthome.haexporter.domain.value.strategy.WorkUnit
import org.apache.logging.log4j.kotlin.Logging
import java.time.Instant

/**
 * There are two types/strategies of processing:
 * 1. If one of the conditions is met:
 *      a) one of the entities has no data in the state repository
 *      b) time difference between the oldest and newest state is greater than defined threshold
 *    In that case state processing switches to the "channeled" mode where entities are processed
 *    one after another up to the specified limit.
 * 2. Normal processing - get the minimum from state repository
 */
class ProcessingStrategyResolver(
    private val stateRepository: StateRepository,
    private val properties: ProcessingProperties
) {

    suspend fun resolve(states: List<EntityStateTime>): ProcessingStrategy {
        if (states.isEmpty())
            return EmptyStrategy

        val lags = states.map { EntityStateLag(it.entity, it.lastUpdated, stateRepository.getLastStateTime(it.entity)) }
        return if (shouldUseChanneledStrategy(lags)) {
            val strategy = ChanneledStrategy(lags)
            logger.info { "Using channeled strategy with start time at ${strategy.startTime}" }
            strategy
        } else {
            val oldestStoredStateTime = lags.mapNotNull { it.storedState }.min()
            logger.info { "Using simple strategy with start time at $oldestStoredStateTime" }
            SimpleStrategy(oldestStoredStateTime)
        }
    }

    private fun shouldUseChanneledStrategy(lags: Collection<EntityStateLag>): Boolean {
        val lagsAboveThreshold = lags.filter { it.value > properties.lagThreshold }
        return if (lagsAboveThreshold.isNotEmpty()) {
            logExceededLagThreshold(lagsAboveThreshold)
            true
        } else false
    }

    private fun logExceededLagThreshold(lags: Collection<EntityStateLag>) {
        logger.debug {
            val items = lags
                .sortedByDescending { it.value }
                .joinToString(separator = "\n") { "  - ${it.entity} - ${it.stringValue}" }
            "Lag threshold has been exceeded by the following entities: \n$items"
        }
    }

    private class SimpleStrategy(private val from: Instant) : ProcessingStrategy {
        override fun getWorkUnits(): List<WorkUnit> = listOf(WorkUnit(from, null))
    }

    private object EmptyStrategy : ProcessingStrategy {
        override fun getWorkUnits(): List<WorkUnit> = emptyList()

    }

    companion object : Logging
}
