package dev.drzepka.smarthome.haexporter.domain.value.strategy

import dev.drzepka.smarthome.haexporter.domain.value.EntityStateLag
import java.time.Instant

class ChanneledStrategy(private val lags: Collection<EntityStateLag>) : ProcessingStrategy {
    val startTime: Instant?
        get() = getWorkUnits().firstOrNull()?.from

    override fun getWorkUnits(): List<WorkUnit> = lags
        .sortedByDescending { it.value }
        .map { WorkUnit(it.storedState ?: Instant.EPOCH, setOf(it.entity)) }
}
