package dev.drzepka.smarthome.haexporter.infrastructure.properties

data class InfluxDBDataSourceProperties(
    val url: String,
    val bucket: String,
    val exportStatusBucket: String?,
    val org: String,
    val token: String,
)
