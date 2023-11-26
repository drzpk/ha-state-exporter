package dev.drzepka.smarthome.haexporter.domain.value

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

data class EntityStateLag(val entity: EntityId, val mostRecentState: Instant, val storedState: Instant?) {
    val value: Duration
        get() {
            if (storedState == null)
                return FOREVER

            if (storedState.isAfter(mostRecentState))
                throw IllegalStateException("Stored state of entity $entity is after most recent state ($storedState > $mostRecentState)")

            return Duration.between(storedState, mostRecentState)
        }

    val stringValue: String
        get() = value.let { if (it == FOREVER) "infinite" else it.toString() }
}

private val FOREVER = ChronoUnit.FOREVER.duration
