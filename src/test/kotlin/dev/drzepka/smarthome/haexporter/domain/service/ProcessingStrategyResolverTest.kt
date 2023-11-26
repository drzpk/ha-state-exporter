package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.properties.ProcessingProperties
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import dev.drzepka.smarthome.haexporter.domain.value.EntityStateTime
import dev.drzepka.smarthome.haexporter.domain.value.strategy.ChanneledStrategy
import dev.drzepka.smarthome.haexporter.domain.value.strategy.WorkUnit
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class ProcessingStrategyResolverTest {

    private val repository = mockk<StateRepository>()
    private val properties = ProcessingProperties(lagThreshold = Duration.ofMinutes(5))

    private val resolver = ProcessingStrategyResolver(repository, properties)

    @Test
    fun `should resolve empty strategy if there are no states`() = runBlocking {
        val workUnits = resolver.resolve(emptyList()).getWorkUnits()
        then(workUnits).isEmpty()
    }

    @Test
    fun `should resolve simple strategy if all lags don't exceed threshold`() = runBlocking {
        val baseStateTime = Instant.now()
        val states = listOf(
            EntityStateTime(ENTITY_1, baseStateTime + Duration.ofMinutes(2)),
            EntityStateTime(ENTITY_2, baseStateTime + Duration.ofMinutes(4)),
            EntityStateTime(ENTITY_3, baseStateTime + Duration.ofMinutes(12))
        )

        coEvery { repository.getLastStateTime(ENTITY_1) } returns baseStateTime + Duration.ofMinutes(1)
        coEvery { repository.getLastStateTime(ENTITY_2) } returns baseStateTime + Duration.ofMinutes(4)
        coEvery { repository.getLastStateTime(ENTITY_3) } returns baseStateTime + Duration.ofMinutes(7)

        val workUnits = resolver.resolve(states).getWorkUnits()

        then(workUnits).containsExactly(WorkUnit(baseStateTime + Duration.ofMinutes(1), null))
        Unit
    }

    @Test
    fun `should resolve channeled strategy if at least one lag is above threshold`() = runBlocking{
        val baseStateTime = Instant.now()
        val states = listOf(
            EntityStateTime(ENTITY_1, baseStateTime + Duration.ofMinutes(8)),
            EntityStateTime(ENTITY_2, baseStateTime + Duration.ofMinutes(15)),
            EntityStateTime(ENTITY_3, baseStateTime + Duration.ofMinutes(21))
        )

        coEvery { repository.getLastStateTime(ENTITY_1) } returns baseStateTime + Duration.ofMinutes(5)
        coEvery { repository.getLastStateTime(ENTITY_2) } returns baseStateTime + Duration.ofMinutes(10)
        coEvery { repository.getLastStateTime(ENTITY_3) } returns baseStateTime + Duration.ofMinutes(15)

        val strategy = resolver.resolve(states)

        then(strategy).isInstanceOf(ChanneledStrategy::class.java)
        then(strategy.getWorkUnits().size).isGreaterThan(1)
        Unit
    }

    @Test
    fun `should resolve channeled strategy if at least one lag is infinite`() = runBlocking {
        val baseStateTime = Instant.now()
        val states = listOf(
            EntityStateTime(ENTITY_1, baseStateTime + Duration.ofMinutes(8)),
            EntityStateTime(ENTITY_2, baseStateTime + Duration.ofMinutes(15)),
            EntityStateTime(ENTITY_3, baseStateTime + Duration.ofMinutes(21))
        )

        coEvery { repository.getLastStateTime(ENTITY_1) } returns baseStateTime + Duration.ofMinutes(5)
        coEvery { repository.getLastStateTime(ENTITY_2) } returns baseStateTime + Duration.ofMinutes(10)
        coEvery { repository.getLastStateTime(ENTITY_3) } returns null

        val strategy = resolver.resolve(states)

        then(strategy).isInstanceOf(ChanneledStrategy::class.java)
        then(strategy.getWorkUnits().size).isGreaterThan(1)
        Unit
    }

    companion object {
        private val ENTITY_1 = EntityId("class", "dev", "sensor-1")
        private val ENTITY_2 = EntityId("class", "dev", "sensor-2")
        private val ENTITY_3 = EntityId("class", "dev", "sensor-3")
    }
}
