package dev.drzepka.smarthome.haexporter.application.model

import java.time.Instant

data class SourceState(
    val id: Long,
    val entityId: String,
    val state: String,
    val lastUpdated: Instant
)
