package dev.drzepka.smarthome.haexporter.infrastructure.provider

import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.domain.util.trimToSeconds
import dev.drzepka.smarthome.haexporter.infrastructure.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.trait.MariaDBTrait
import dev.drzepka.smarthome.haexporter.trait.MariaDBTrait.Companion.createMariaDBContainer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@Timeout(10)
@Testcontainers
class MariaDBHomeAssistantStateProviderTest : MariaDBTrait {

    @Container
    override val mariaDBContainer: MariaDBContainer<*> = createMariaDBContainer()

    private lateinit var connectionProvider: SQLConnectionProvider
    private lateinit var stateProvider: HomeAssistantStateProvider

    @BeforeEach
    fun beforeEach() = runBlocking {
        connectionProvider = SQLConnectionProvider(getMariaDBDataSourceProperties())
        stateProvider = SQLHomeAssistantStateProvider(connectionProvider)
        connectionProvider.createSchema()
    }

    @Test
    fun `should get states for given start time, offset, and limit`() = runBlocking {
        val time = Instant.now().trimToSeconds()
        connectionProvider.createState(1, "entity_1", "state_1", time.plusSeconds(1))
        connectionProvider.createState(2, "entity_2", "state_2", time.plusSeconds(2))
        connectionProvider.createState(3, "entity_3", "state_3", time.plusSeconds(3))
        connectionProvider.createState(4, "entity_4", "state_4", time.plusSeconds(4))
        connectionProvider.createState(5, "entity_5", "state_5", time.plusSeconds(5))

        val states = stateProvider.getStates(time.plusSeconds(2), 1, 2)

        then(states).hasSize(2)
        then(states[0]).isEqualTo(SourceState(3, "entity_3", "state_3", time.plusSeconds(3)))
        then(states[1]).isEqualTo(SourceState(4, "entity_4", "state_4", time.plusSeconds(4)))

        Unit
    }

    @Test
    fun `should get states ordered by last updated time`() = runBlocking {
        val time = Instant.now()
        connectionProvider.createState(1, "id", "state", time.plusSeconds(10))
        connectionProvider.createState(2, "id", "state", time.plusSeconds(7))
        connectionProvider.createState(3, "id", "state", time.plusSeconds(15))
        connectionProvider.createState(4, "id", "state", time.plusSeconds(14))

        val states = stateProvider.getStates(time, 0, 10)

        then(states).hasSize(4)
        then(states.map { it.id }).containsAll(listOf(2, 1, 4, 3))

        Unit
    }
}
