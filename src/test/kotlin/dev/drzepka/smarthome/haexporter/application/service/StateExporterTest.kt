package dev.drzepka.smarthome.haexporter.application.service

import dev.drzepka.smarthome.haexporter.application.model.EntityMetadata
import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.properties.ExporterProperties
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.domain.properties.EntitiesProperties
import dev.drzepka.smarthome.haexporter.domain.properties.EntityProperties
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.service.EntityIdResolver
import dev.drzepka.smarthome.haexporter.domain.value.EntitySelector
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class StateExporterTest {

    private val entitiesProperties = listOf(
        EntityProperties(EntitySelector(device = "temperature"), "temperature")
    )

    private val stateRepository = mockk<StateRepository>()
    private val metadataProvider = mockk<HomeAssistantEntityMetadataProvider>()
    private val stateProvider = mockk<HomeAssistantStateProvider>()
    private val statePipeline = mockk<StatePipeline>()

    private val flowSlot = slot<Flow<SourceState>>()

    private val stateExporter = StateExporter(
        stateRepository,
        metadataProvider,
        ExporterProperties(batchSize = 500),
        EntityIdResolver(EntitiesProperties(entitiesProperties)),
        stateProvider,
        statePipeline
    )

    @Test
    fun `should export states from beginning when there are no existing states in repository`() = runBlocking {
        val sourceStates = listOf(
            SourceState(1, "sensor.temperature_outside", "12.34", Instant.now()),
            SourceState(2, "sensor.temperature_outside", "56.78", Instant.now()),
            SourceState(4, "sensor.temperature_inside", "22.34", Instant.now()),
        )

        coEvery { stateRepository.getLastStateTime(any()) } returns null
        coEvery { metadataProvider.getEntityMetadata() } returns listOf(
            EntityMetadata("sensor.temperature_outside", Instant.now()),
            EntityMetadata("sensor.temperature_inside", Instant.now()),
            EntityMetadata("sensor.unknown_entity", Instant.now())
        )
        coEvery { stateProvider.getStates(any(), any(), any()) } returns sourceStates
        coEvery { statePipeline.execute(capture(flowSlot)) } just Runs

        stateExporter.export()

        then(flowSlot.captured.toList()).containsAll(sourceStates)
        coVerify { stateProvider.getStates(Instant.EPOCH, 0, 500) }
    }

    @Test
    fun `should export states starting at the time of last previously exported state`() = runBlocking {
        val sourceStates = listOf(
            SourceState(1, "sensor.temperature_outside", "12.34", Instant.now()),
            SourceState(2, "sensor.unknown_1", "22.34", Instant.now()),
            SourceState(3, "sensor.temperature_inside", "22.34", Instant.now()),
            SourceState(4, "sensor.unknown_2", "22.34", Instant.now())
        )

        val time = Instant.now().minusSeconds(100)
        coEvery { stateRepository.getLastStateTime(match { it.toString() == "sensor.temperature_outside" }) } returns time.plusSeconds(20)
        coEvery { stateRepository.getLastStateTime(match { it.toString() == "sensor.unknown_1" }) } returns time.plusSeconds(50)
        coEvery { stateRepository.getLastStateTime(match { it.toString() == "sensor.temperature_inside" }) } returns time.plusSeconds(14)
        coEvery { stateRepository.getLastStateTime(match { it.toString() == "sensor.unknown_2" }) } returns time.plusSeconds(1)

        coEvery { metadataProvider.getEntityMetadata() } returns listOf(
            EntityMetadata("sensor.temperature_outside", Instant.now()),
            EntityMetadata("sensor.temperature_inside", Instant.now()),
            EntityMetadata("sensor.unknown_entity", Instant.now())
        )
        coEvery { stateProvider.getStates(any(), any(), any()) } returns sourceStates
        coEvery { statePipeline.execute(capture(flowSlot)) } just Runs

        stateExporter.export()

        then(flowSlot.captured.toList()).containsAll(sourceStates)
        coVerify { stateProvider.getStates(time.plusSeconds(20), 0, 500) }
    }
}
