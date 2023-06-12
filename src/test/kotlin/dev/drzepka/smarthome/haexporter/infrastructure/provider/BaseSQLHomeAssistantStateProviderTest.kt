package dev.drzepka.smarthome.haexporter.infrastructure.provider

import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.domain.util.blockingGet
import dev.drzepka.smarthome.haexporter.domain.util.toEpochSecondDouble
import dev.drzepka.smarthome.haexporter.domain.util.trimToSeconds
import dev.drzepka.smarthome.haexporter.infrasctucture.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrasctucture.properties.SQLDataSourceProperties
import dev.drzepka.smarthome.haexporter.infrasctucture.provider.SQLHomeAssistantStateProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

@Suppress("SqlNoDataSourceInspection", "SqlResolve")
@Timeout(5)
@Testcontainers
abstract class BaseSQLHomeAssistantStateProviderTest {

    private lateinit var connectionProvider: SQLConnectionProvider
    private lateinit var repository: HomeAssistantStateProvider

    abstract fun getContainer(): JdbcDatabaseContainer<*>
    abstract fun getProperties(): SQLDataSourceProperties

    @BeforeEach
    fun beforeEach() = runBlocking {
        connectionProvider = SQLConnectionProvider(getProperties())
        repository = SQLHomeAssistantStateProvider(connectionProvider)


        connectionProvider
            .getConnection()
            .createStatement(CREATE_TABLE_QUERY)
            .execute()
            .awaitFirst()

        Unit
    }

    @Test
    fun `should get states for given start time and limit`() = runBlocking {
        val time = Instant.now().trimToSeconds()
        createState(1, "entity_1", "state_1", time.plusSeconds(1))
        createState(2, "entity_2", "state_2", time.plusSeconds(2))
        createState(3, "entity_3", "state_3", time.plusSeconds(3))
        createState(4, "entity_4", "state_4", time.plusSeconds(4))
        createState(5, "entity_5", "state_5", time.plusSeconds(5))

        val states = repository.getStates(time.plusSeconds(2), 2).toList()

        then(states).hasSize(2)
        then(states[0]).isEqualTo(SourceState(2, "entity_2", "state_2", time.plusSeconds(2)))
        then(states[1]).isEqualTo(SourceState(3, "entity_3", "state_3", time.plusSeconds(3)))

        Unit
    }

    @Test
    fun `should get states ordered by last updated time`() = runBlocking {
        val time = Instant.now()
        createState(1, "id", "state", time.plusSeconds(10))
        createState(2, "id", "state", time.plusSeconds(7))
        createState(3, "id", "state", time.plusSeconds(15))
        createState(4, "id", "state", time.plusSeconds(14))

        val states = repository.getStates(time, 10).toList()

        then(states).hasSize(4)
        then(states.map { it.id }).containsAll(listOf(2, 1, 4, 3))

        Unit
    }

    private suspend fun createState(id: Int, entityId: String, state: String, lastUpdated: Instant) {
        connectionProvider
            .getConnection()
            .createStatement(
                """
                    INSERT INTO states (state_id, entity_id, state, last_updated_ts)
                    VALUES ($id, '$entityId', '$state', ${lastUpdated.toEpochSecondDouble()})
                """.trimIndent()
            )
            .execute()
            .blockingGet()
    }

    companion object {
        private const val CREATE_TABLE_QUERY = """
            CREATE TABLE `states` (
              `state_id` int(11) NOT NULL AUTO_INCREMENT,
              `entity_id` varchar(255) DEFAULT NULL,
              `state` varchar(255) DEFAULT NULL,
              `attributes` longtext DEFAULT NULL,
              `event_id` int(11) DEFAULT NULL,
              `last_changed` datetime(6) DEFAULT NULL,
              `last_changed_ts` double DEFAULT NULL,
              `last_updated` datetime(6) DEFAULT NULL,
              `last_updated_ts` double DEFAULT NULL,
              `old_state_id` int(11) DEFAULT NULL,
              `attributes_id` int(11) DEFAULT NULL,
              `context_id` varchar(36) DEFAULT NULL,
              `context_user_id` varchar(36) DEFAULT NULL,
              `context_parent_id` varchar(36) DEFAULT NULL,
              `origin_idx` smallint(6) DEFAULT NULL,
              PRIMARY KEY (`state_id`),
              KEY `ix_states_last_updated_ts` (`last_updated_ts`),
              KEY `ix_states_context_id` (`context_id`),
              KEY `ix_states_old_state_id` (`old_state_id`),
              KEY `ix_states_event_id` (`event_id`),
              KEY `ix_states_attributes_id` (`attributes_id`),
              KEY `ix_states_entity_id_last_updated_ts` (`entity_id`,`last_updated_ts`)
            ) ENGINE=InnoDB AUTO_INCREMENT=1594106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """
    }
}
