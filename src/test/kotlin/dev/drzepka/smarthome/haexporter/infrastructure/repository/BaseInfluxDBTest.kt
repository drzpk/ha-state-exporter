package dev.drzepka.smarthome.haexporter.infrastructure.repository

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.query.FluxRecord
import com.influxdb.query.dsl.Flux
import dev.drzepka.smarthome.haexporter.infrastructure.properties.InfluxDBDataSourceProperties
import kotlinx.coroutines.delay
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.fail
import org.testcontainers.containers.InfluxDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant

@Testcontainers
@Timeout(15)
abstract class BaseInfluxDBTest {

    @Container
    private val influxDB = InfluxDBContainer(DockerImageName.parse("influxdb:2.7.1"))
        .withAdminToken("admin-token")
        .withOrganization("test")
        .withBucket("test")

    private val client by lazy {
        InfluxDBClientFactory
            .create(influxDB.url, influxDB.adminToken.get().toCharArray(), "test", "test-bucket")
    }

    private val bucket: String
        get() = influxDB.bucket

    protected fun getDataSourceProperties() = InfluxDBDataSourceProperties(
        influxDB.url,
        influxDB.bucket,
        influxDB.organization,
        influxDB.adminToken.get()
    )

    protected suspend fun getRecords(): List<FluxRecord> {
        var attemptsLeft = 20
        var previousSize = -1
        var sizeOccurrences = 0

        do {
            val result = queryRecords()
            if (result.size == previousSize)
                sizeOccurrences++
            else
                sizeOccurrences = 1

            previousSize = result.size

            if (sizeOccurrences == 3)
                return result

            delay(500)
        } while (--attemptsLeft > 0)

        throw IllegalStateException("Attempt limit has been reached")
    }

    protected fun Iterable<FluxRecord>.assertContains(
        time: Instant,
        measurement: String,
        field: String,
        value: Any,
        tags: Map<String, String>
    ) {
        val record = this.firstOrNull { it.time == time && it.measurement == measurement && it.field == field }
            ?: fail("No record found at time=$time with measurement=$measurement and field=$field")

        then(record.value).isEqualTo(value)
        tags.forEach { (key, expected) ->
            val actual = record.getValueByKey(key) ?: fail("No value found for key=$key")
            then(actual).isEqualTo(expected)
        }
    }

    private fun queryRecords(): List<FluxRecord> {
        val flux = Flux
            .from(bucket)
            .range(0L)

        return client.queryApi
            .query(flux.toString())
            .flatMap { it.records }
    }
}
