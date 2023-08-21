package dev.drzepka.smarthome.haexporter.infrastructure.repository

import dev.drzepka.smarthome.haexporter.trait.InfluxDBTrait
import dev.drzepka.smarthome.haexporter.trait.InfluxDBTrait.Companion.createInfluxDBContainer
import org.junit.jupiter.api.Timeout
import org.testcontainers.containers.InfluxDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@Timeout(15)
abstract class BaseInfluxDBTest : InfluxDBTrait {

    @Container
    override val influxDBContainer: InfluxDBContainer<*> = createInfluxDBContainer()
    override val influxDBClient by lazy { createInfluxDBClient() }
}
