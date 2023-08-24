package dev.drzepka.smarthome.haexporter.application.properties

data class ExporterProperties(
    val batchSize: Int = 5_000,
    val processingLimit: Int = 100_000
)
