package dev.drzepka.smarthome.haexporter.domain.value

import java.time.Instant

data class EntityStateTime(val entity: EntityId, val lastUpdated: Instant)
