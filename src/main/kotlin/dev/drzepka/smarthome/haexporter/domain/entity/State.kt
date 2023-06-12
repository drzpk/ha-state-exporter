package dev.drzepka.smarthome.haexporter.domain.entity

import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.domain.value.StateValue
import java.time.Instant

data class State(
    val time: Instant,
    val entity: EntityId,
    val value: StateValue
)
