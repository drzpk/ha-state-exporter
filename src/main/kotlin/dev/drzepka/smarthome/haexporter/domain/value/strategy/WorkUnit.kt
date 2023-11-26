package dev.drzepka.smarthome.haexporter.domain.value.strategy

import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import java.time.Instant

data class WorkUnit(val from: Instant, val entityFilter: Set<EntityId>?)
