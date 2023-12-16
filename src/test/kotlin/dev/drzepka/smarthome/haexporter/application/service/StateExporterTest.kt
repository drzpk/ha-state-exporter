package dev.drzepka.smarthome.haexporter.application.service

import dev.drzepka.smarthome.haexporter.application.model.EntityMetadata
import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.model.SourceStateQuery
import dev.drzepka.smarthome.haexporter.application.properties.ExporterProperties
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.service.EntityIdResolver
import dev.drzepka.smarthome.haexporter.domain.service.ProcessingStrategyResolver
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.domain.value.EntityStateTime
import dev.drzepka.smarthome.haexporter.domain.value.strategy.ProcessingStrategy
import dev.drzepka.smarthome.haexporter.domain.value.strategy.WorkUnit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class StateExporterTest {

    private val strategyResolver = mockk<ProcessingStrategyResolver>()
    private val metadataProvider = mockk<HomeAssistantEntityMetadataProvider>()
    private val entityIdResolver = mockk<EntityIdResolver>()
    private val stateProvider = mockk<HomeAssistantStateProvider>()
    private val statePipeline = mockk<StatePipeline>()
    private val stateRepository = mockk<StateRepository>()

    private val flowSlot = slot<Flow<SourceState>>()

    private val stateExporter = StateExporter(
        strategyResolver,
        metadataProvider,
        entityIdResolver,
        stateProvider,
        statePipeline,
        stateRepository,
        ExporterProperties(batchSize = 500)
    )

    @Test
    fun `should manage exporting process`() = runBlocking {
        val sourceStates = listOf(
            SourceState(1, ENTITY_ID_1_STR, "12.34", TIME_1),
            SourceState(2, ENTITY_ID_1_STR, "56.78", TIME_2),
            SourceState(4, ENTITY_ID_2_STR, "22.34", TIME_3),
        )

        coEvery { metadataProvider.getEntityMetadata() } returns listOf(
            EntityMetadata(ENTITY_ID_1_STR, TIME_1),
            EntityMetadata(ENTITY_ID_2_STR, TIME_2),
            EntityMetadata("sensor.unknown_entity", TIME_3)
        )

        every { entityIdResolver.resolve(any()) } returns null
        every { entityIdResolver.resolve(ENTITY_ID_1_STR) } returns ENTITY_ID_1
        every { entityIdResolver.resolve(ENTITY_ID_2_STR) } returns ENTITY_ID_2

        val workUnitTime = Instant.now()
        val strategy = TestStrategy(listOf(WorkUnit(workUnitTime, setOf(ENTITY_ID_1, ENTITY_ID_2))))
        coEvery {
            strategyResolver.resolve(listOf(EntityStateTime(ENTITY_ID_1, TIME_1), EntityStateTime(ENTITY_ID_2, TIME_2)))
        } returns strategy

        val stateFlow = emptyFlow<State>()
        coEvery { stateProvider.getStates(any()) } returns sourceStates
        coEvery { statePipeline.execute(capture(flowSlot)) } returns stateFlow

        stateExporter.export()

        then(flowSlot.captured.toList()).containsAll(sourceStates)
        coVerify {
            stateProvider.getStates(SourceStateQuery(workUnitTime, setOf(ENTITY_ID_1_STR, ENTITY_ID_2_STR), 0, 500))
            stateRepository.save(refEq(stateFlow))
        }
    }

    companion object {
        private const val ENTITY_ID_1_STR = "sensor.temperature_outside"
        private const val ENTITY_ID_2_STR = "sensor.temperature_inside"

        private val ENTITY_ID_1 = EntityId("sensor", "temperature", "outside")
        private val ENTITY_ID_2 = EntityId("sensor", "temperature", "inside")

        private val TIME_1 = Instant.now().plusSeconds(1)
        private val TIME_2 = Instant.now().plusSeconds(2)
        private val TIME_3 = Instant.now().plusSeconds(3)
    }
}

private class TestStrategy(val units: List<WorkUnit>) : ProcessingStrategy {
    override fun getWorkUnits(): List<WorkUnit> = units
}
