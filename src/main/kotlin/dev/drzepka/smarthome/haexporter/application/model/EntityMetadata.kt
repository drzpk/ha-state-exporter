package dev.drzepka.smarthome.haexporter.application.model

import java.time.Instant

data class EntityMetadata(val entityId: String, val lastChanged: Instant)
