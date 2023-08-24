package dev.drzepka.smarthome.haexporter

import dev.drzepka.smarthome.haexporter.trait.InfluxDBTrait
import dev.drzepka.smarthome.haexporter.trait.MariaDBTrait
import dev.drzepka.smarthome.haexporter.trait.MariaDBTrait.Companion.createMariaDBContainer
import org.testcontainers.containers.InfluxDBContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

//@Testcontainers
//abstract class BaseE2ETest : MariaDBTrait, InfluxDBTrait {
//
//    @Container
//    override val mariaDBContainer: MariaDBContainer<*> = createMariaDBContainer()
//}