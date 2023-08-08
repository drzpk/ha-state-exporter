package dev.drzepka.smarthome.haexporter.trait

import dev.drzepka.smarthome.haexporter.domain.util.blockingGet
import dev.drzepka.smarthome.haexporter.domain.util.toEpochSecondDouble
import dev.drzepka.smarthome.haexporter.infrastructure.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrastructure.properties.SQLDataSourceProperties
import kotlinx.coroutines.reactive.awaitFirst
import org.testcontainers.containers.MariaDBContainer
import java.time.Instant

@Suppress("SqlNoDataSourceInspection", "SqlResolve")
interface MariaDBTrait {
    val mariaDBContainer: MariaDBContainer<*>

    fun getMariaDBDataSourceProperties(): SQLDataSourceProperties = SQLDataSourceProperties(
        "mariadb",
        mariaDBContainer.host,
        mariaDBContainer.firstMappedPort,
        mariaDBContainer.username,
        mariaDBContainer.password,
        mariaDBContainer.databaseName
    )

    suspend fun SQLConnectionProvider.createSchema() {
        getConnection()
            .createStatement(CREATE_TABLE_QUERY)
            .execute()
            .awaitFirst()
    }

    suspend fun SQLConnectionProvider.createState(id: Int, entityId: String, state: String, lastUpdated: Instant) {
        getConnection()
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

        fun createMariaDBContainer(): MariaDBContainer<*> = MariaDBContainer("mariadb")
    }
}
