package dev.drzepka.smarthome.haexporter.domain.properties

import java.time.Duration

data class ProcessingProperties(
    val lagThreshold: Duration = Duration.ofMinutes(10)
)
