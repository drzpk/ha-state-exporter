package dev.drzepka.smarthome.haexporter.application.model

import java.time.Instant

data class SourceStateQuery(
    val from: Instant,
    val entities: Set<String>?,
    val offset: Int,
    val limit: Int
)
