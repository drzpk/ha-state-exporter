package dev.drzepka.smarthome.haexporter.application.service

import dev.drzepka.smarthome.haexporter.application.configuration.applicationModule
import dev.drzepka.smarthome.haexporter.application.configuration.domainModule
import dev.drzepka.smarthome.haexporter.application.configuration.influxDBModule
import dev.drzepka.smarthome.haexporter.application.configuration.mariaDBModule
import dev.drzepka.smarthome.haexporter.application.model.EntityMetadata
import dev.drzepka.smarthome.haexporter.application.properties.EntitySchema
import dev.drzepka.smarthome.haexporter.application.properties.RootProperties
import dev.drzepka.smarthome.haexporter.application.properties.SchemaProperties
import dev.drzepka.smarthome.haexporter.application.properties.ValueType
import dev.drzepka.smarthome.haexporter.application.provider.ConfigurationPropertiesProvider
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.properties.EntityProperties
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.util.trimToSeconds
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.domain.value.EntitySelector
import dev.drzepka.smarthome.haexporter.domain.value.NumericStateValue
import dev.drzepka.smarthome.haexporter.infrastructure.database.InfluxDBClientProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrastructure.properties.InfluxDBDataSourceProperties
import dev.drzepka.smarthome.haexporter.infrastructure.provider.DirectConfigurationProvider
import dev.drzepka.smarthome.haexporter.trait.InfluxDBTrait
import dev.drzepka.smarthome.haexporter.trait.InfluxDBTrait.Companion.createInfluxDBContainer
import dev.drzepka.smarthome.haexporter.trait.MariaDBTrait
import dev.drzepka.smarthome.haexporter.trait.MariaDBTrait.Companion.createMariaDBContainer
import io.mockk.coEvery
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.testcontainers.containers.InfluxDBContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@Testcontainers
@ExtendWith(MockKExtension::class)
internal class StateExporterE2ETest : MariaDBTrait, InfluxDBTrait, KoinTest {

    @Container
    override val mariaDBContainer: MariaDBContainer<*> = createMariaDBContainer()

    @Container
    override val influxDBContainer: InfluxDBContainer<*> = createInfluxDBContainer()

    override val influxDBClient by lazy { createInfluxDBClient() }
    private lateinit var connectionProvider: SQLConnectionProvider

    private val properties by lazy {
        RootProperties(
            homeAssistant = getMariaDBDataSourceProperties(),
            influxDB = InfluxDBDataSourceProperties("", "", "", ""),
            entities = listOf(
                EntityProperties(EntitySelector(device = "temperature"), "temperature"),
                EntityProperties(EntitySelector(`class` = "binary_sensor", device = "door", sensor = "state"), "door")
            ),
            schemas = listOf(
                SchemaProperties(
                    "temperature",
                    "temp"
                ),
                SchemaProperties(
                    "door",
                    "doors",
                    entities = listOf(
                        EntitySchema("state", type = ValueType.STRING)
                    )
                )
            )
        )
    }

    private val metadataProvider = mockk<HomeAssistantEntityMetadataProvider>()

    private val stateRepository by inject<StateRepository>()
    private val stateExporter by inject<StateExporter>()

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            connectionProvider = SQLConnectionProvider(getMariaDBDataSourceProperties())
            connectionProvider.createSchema()
        }

        startKoin {
            val testModule = module {
                single { connectionProvider }
                single { metadataProvider }
                single { InfluxDBClientProvider(getDataSourceProperties()) }
                single<ConfigurationPropertiesProvider> { DirectConfigurationProvider(properties) }
            }

            modules(mariaDBModule, influxDBModule, domainModule, applicationModule, testModule)
        }
    }

    @AfterEach
    fun afterEach() {
        stopKoin()
    }

    @Test
    fun `should export states when there are no existing states in repository`() = runBlocking {
        val time = Instant.now().trimToSeconds().minusSeconds(100)

        connectionProvider.createState(1, "binary_sensor.door_state", "open", time.plusSeconds(6))
        connectionProvider.createState(2, "sensor.temperature_inside", "21.33", time.plusSeconds(1))
        connectionProvider.createState(3, "sensor.temperature_inside", "22.04", time.plusSeconds(2))
        connectionProvider.createState(4, "sensor.temperature_outside", "24.43", time.plusSeconds(3))
        connectionProvider.createState(5, "sensor.temperature", "25.92", time.plusSeconds(4))
        connectionProvider.createState(6, "sensor.unknown_entity", "test", time.plusSeconds(5))

        coEvery { metadataProvider.getEntityMetadata() } returns listOf(
            EntityMetadata("sensor.temperature_outside", Instant.now()),
            EntityMetadata("sensor.temperature_inside", Instant.now()),
            EntityMetadata("sensor.unknown_entity", Instant.now()),
            EntityMetadata("binary_sensor.door_state", Instant.now())
        )

        stateExporter.export()

        val records = getRecords()
        then(records).hasSize(5)

        records.assertContains(time.plusSeconds(6), "doors", "state", "open", tags("door", "state", "binary_sensor"))
        records.assertContains(time.plusSeconds(1), "temp", "inside", 21.33, tags("temperature", "inside"))
        records.assertContains(time.plusSeconds(2), "temp", "inside", 22.04, tags("temperature", "inside"))
        records.assertContains(time.plusSeconds(3), "temp", "outside", 24.43, tags("temperature", "outside"))
        records.assertContains(time.plusSeconds(4), "temp", "value", 25.92, tags("temperature", null))
    }

    @Test
    fun `should export states starting at the time of last previously exported state`() = runBlocking {
        val time = Instant.now().trimToSeconds().minusSeconds(100)

        val preExistingStates = listOf(
            State(time.plusSeconds(10), EntityId("sensor", "temperature", "inside"), "temp", NumericStateValue(23.1)),
            State(time.plusSeconds(10), EntityId("sensor", "temperature", "outside"), "temp", NumericStateValue(12.2))
        )
        stateRepository.save(preExistingStates.asFlow())

        connectionProvider.createState(1, "binary_sensor.door_state", "open", time.plusSeconds(6))
        connectionProvider.createState(2, "sensor.temperature_inside", "21.01", time.plusSeconds(1))
        connectionProvider.createState(3, "sensor.temperature_inside", "22.02", time.plusSeconds(10))
        connectionProvider.createState(4, "sensor.temperature_inside", "60.02", time.plusSeconds(11))
        connectionProvider.createState(5, "sensor.temperature_outside", "29.31", time.plusSeconds(12))

        coEvery { metadataProvider.getEntityMetadata() } returns listOf(
            EntityMetadata("sensor.temperature_inside", Instant.now()),
            EntityMetadata("sensor.temperature_outside", Instant.now()),
            EntityMetadata("binary_sensor.door_state", Instant.now())
        )

        stateExporter.export()

        val records = getRecords()
        then(records).hasSize(4)

        records.assertContains(time.plusSeconds(10), "temp", "inside", 22.02, tags("temperature", "inside")) // duplicates are overridden
        records.assertContains(time.plusSeconds(11), "temp", "inside", 60.02, tags("temperature", "inside"))
        records.assertContains(time.plusSeconds(10), "temp", "outside", 12.2, tags("temperature", "outside"))
        records.assertNotContains(time.plusSeconds(1), "temp", "inside")
    }

    private fun tags(dev: String, sensor: String?, clazz: String = "sensor"): Map<String, String> =
        mutableMapOf("class" to clazz, "device" to dev).also {
            if (sensor != null)
                it["sensor"] = sensor
        }
}
