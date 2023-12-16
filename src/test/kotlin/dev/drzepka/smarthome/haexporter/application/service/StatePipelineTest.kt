package dev.drzepka.smarthome.haexporter.application.service

import dev.drzepka.smarthome.haexporter.application.configuration.applicationModule
import dev.drzepka.smarthome.haexporter.application.configuration.domainModule
import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.properties.EntitySchema
import dev.drzepka.smarthome.haexporter.application.properties.SchemaProperties
import dev.drzepka.smarthome.haexporter.application.properties.SchemasProperties
import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.properties.EntitiesProperties
import dev.drzepka.smarthome.haexporter.domain.properties.EntityProperties
import dev.drzepka.smarthome.haexporter.domain.service.StateValueConverter
import dev.drzepka.smarthome.haexporter.domain.util.trimToSeconds
import dev.drzepka.smarthome.haexporter.domain.value.BooleanStateValue
import dev.drzepka.smarthome.haexporter.domain.value.DefaultValueMapping
import dev.drzepka.smarthome.haexporter.domain.value.DoubleStateValue
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.domain.value.EntitySelector
import dev.drzepka.smarthome.haexporter.domain.value.StateMapping
import dev.drzepka.smarthome.haexporter.domain.value.StateMappings
import dev.drzepka.smarthome.haexporter.domain.value.StringStateValue
import dev.drzepka.smarthome.haexporter.domain.value.ValueMapping
import dev.drzepka.smarthome.haexporter.domain.value.ValueType
import dev.drzepka.smarthome.haexporter.trait.KoinTrait
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
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
import java.time.Duration
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class StatePipelineTest : KoinTrait, KoinTest {

    private val entityProperties = listOf(
        EntityProperties(EntitySelector(devices = listOf("outside")), "outside_sensor_schema"),
        EntityProperties(EntitySelector(devices = listOf("energy", "old_energy")), "energy"),
    )

    private val schemaProperties = listOf(
        SchemaProperties(
            name = "outside_sensor_schema",
            influxMeasurementName = "outside_data",
            entities = listOf(
                EntitySchema("temperature_str", type = ValueType.STRING),
                EntitySchema("temperature", type = ValueType.FLOAT, ignoredValues = listOf("5.0"))
            )
        ),
        SchemaProperties(
            name = "energy",
            influxMeasurementName = "energy",
            entities = listOf(
                EntitySchema("availability", type = ValueType.STRING, stateMapping = "availability")
            ),
            deviceNameMapping = mapOf(
                "old_energy" to "energy"
            )
        )
    )

    private val stateMappings = listOf(
        StateMapping(
            "availability",
            ValueType.BOOLEAN,
            listOf(ValueMapping("on", "true")),
            DefaultValueMapping("false")
        )
    )

    private val stateValueConverter = spyk(StateValueConverter())

    private val statePipeline: StatePipeline by inject()

    @BeforeEach
    fun beforeEach() {
        startKoin {
            val testModule = module {
                single { EntitiesProperties(entityProperties) }
                single { SchemasProperties(schemaProperties) }
                single { StateMappings(stateMappings) }
                single { stateValueConverter }
            }

            modules(domainModule, applicationModule, defaultConfigurationModule, testModule)
        }
    }

    @AfterEach
    fun afterEach() {
        stopKoin()
    }

    @Test
    fun `should save states`() = runBlocking {
        val time = Instant.now().trimToSeconds()
        val sourceStates = listOf(
            SourceState(1, "sensor.outside_temperature_str", "32.12", time + Duration.ofSeconds(1)),
            SourceState(2, "sensor.outside_temperature", "25.4", time + Duration.ofSeconds(2)),
            SourceState(3, "sensor.outside_temperature", "5.0", time + Duration.ofSeconds(3)),
            SourceState(4, "sensor.energy_availability", "on", time + Duration.ofSeconds(4)),
            SourceState(5, "sensor.energy_availability", "off", time + Duration.ofSeconds(5)),
            SourceState(6, "sensor.energy_availability", "unknown", time + Duration.ofSeconds(6)),
        )

        val states = statePipeline.execute(sourceStates.asFlow()).toList()

        then(states).hasSize(5)
        then(states).containsExactlyInAnyOrder(
            State(time + Duration.ofSeconds(1), EntityId("sensor", "outside", "temperature_str"), "outside_data", StringStateValue("32.12")),
            State(time + Duration.ofSeconds(2), EntityId("sensor", "outside", "temperature"), "outside_data", DoubleStateValue(25.4)),
            State(time + Duration.ofSeconds(4), EntityId("sensor", "energy", "availability"), "energy", BooleanStateValue(true)),
            State(time + Duration.ofSeconds(5), EntityId("sensor", "energy", "availability"), "energy", BooleanStateValue(false)),
            State(time + Duration.ofSeconds(6), EntityId("sensor", "energy", "availability"), "energy", BooleanStateValue(false))
        )

        Unit
    }

    @Test
    fun `should map device name`() = runBlocking {
        val time = Instant.now().trimToSeconds()
        val sourceStates = listOf(
            SourceState(1, "sensor.energy_availability", "on", time + Duration.ofSeconds(1)),
            SourceState(1, "sensor.old_energy_availability", "off", time + Duration.ofSeconds(2))
        )

        val states = statePipeline.execute(sourceStates.asFlow()).toList()

        then(states).hasSize(2)
        then(states).containsExactlyInAnyOrder(
            State(time + Duration.ofSeconds(1), EntityId("sensor", "energy", "availability"), "energy", BooleanStateValue(true)),
            State(time + Duration.ofSeconds(2), EntityId("sensor", "energy", "availability"), "energy", BooleanStateValue(false))
        )

        Unit
    }

    @Test
    fun `pipeline should continue on error in a single state`() = runBlocking {
        val time = Instant.now().trimToSeconds()
        val sourceStates = listOf(
            SourceState(1, "sensor.outside_temperature", "99.99", time + Duration.ofSeconds(1)),
            SourceState(2, "sensor.outside_temperature", "12.34", time + Duration.ofSeconds(2))
        )

        every { stateValueConverter.convert(eq("99.99"), any()) } throws IllegalArgumentException("Something bad happened")

        val states = statePipeline.execute(sourceStates.asFlow()).toList()

        then(states).hasSize(1)
        then(states).containsExactlyInAnyOrder(
            State(time + Duration.ofSeconds(2), EntityId("sensor", "outside", "temperature"), "outside_data", DoubleStateValue(12.34)),
        )

        Unit
    }
}
