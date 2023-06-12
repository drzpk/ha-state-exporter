package dev.drzepka.smarthome.haexporter.infrasctucture.properties

data class InfluxDBDataSourceProperties(
    val url: String,
    val bucket: String,
    val org: String,
    val token: String,
)
