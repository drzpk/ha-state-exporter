package dev.drzepka.smarthome.haexporter.application.properties

import dev.drzepka.smarthome.haexporter.domain.properties.ProcessingProperties
import java.time.Duration

data class ExporterProperties(
    val interval: Duration = Duration.ofSeconds(15),
    val batchSize: Int = 5_000,
    val processingLimit: Int = 100_000,
    val processing: ProcessingProperties = ProcessingProperties()
)
