package dev.drzepka.smarthome.haexporter.domain.entity

import java.time.Instant

data class ExportStatus(
    val time: Instant,
    val startedAt: Instant,
    val finishedAt: Instant,
    val success: Boolean,
    val exception: String?,
    val loadedStates: Int,
    val savedStates: Int,
    val entities: Int,
    val firstStateTime: Instant?,
    val lastStateTime: Instant?
)
