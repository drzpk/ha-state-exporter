package dev.drzepka.smarthome.haexporter.trait

import dev.drzepka.smarthome.haexporter.domain.util.toEpochSecondDouble
import dev.drzepka.smarthome.haexporter.infrastructure.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrastructure.properties.SQLDataSourceProperties
import dev.drzepka.smarthome.haexporter.infrastructure.provider.executeAsync
import dev.drzepka.smarthome.haexporter.infrastructure.provider.executeQueryAsync
import org.testcontainers.containers.MariaDBContainer
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

private val metadataIdCounter = AtomicInteger(1)

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
        listOf(CREATE_STATES_TABLE_QUERY, CREATE_STATES_META_TABLE_QUERY).forEach { stmt ->
            acquireConnection {
                it.createStatement().executeAsync(stmt)
            }
        }
    }

    suspend fun SQLConnectionProvider.createState(id: Int, entityId: String, state: String, lastUpdated: Instant) {
        println("Creating state: $entityId")
        var metadataId = acquireConnection {
            val result = it.createStatement()
                .executeQueryAsync("SELECT metadata_id FROM states_meta WHERE entity_id = '$entityId'")

            var metadataId: Int? = null
            if (result.first())
                metadataId = result.getInt("metadata_id")

            metadataId
        }

        println("metadata id: $metadataId")

        if (metadataId == null) {
            metadataId = metadataIdCounter.getAndIncrement()
            acquireConnection {
                it.createStatement()
                    .executeAsync("INSERT INTO states_meta (metadata_id, entity_id) VALUES ($metadataId, '$entityId')")
            }
        }

        println("Inserted into states meta")

        acquireConnection {
            it.createStatement()
                .executeAsync(
                    """
                    INSERT INTO states (state_id, state, last_updated_ts, metadata_id)
                    VALUES ($id, '$state', ${lastUpdated.toEpochSecondDouble()}, $metadataId)
                """.trimIndent()
                )
        }

        println("Finished creating state: $entityId")
    }

    companion object {
        private const val CREATE_STATES_TABLE_QUERY = """
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
              `context_id_bin` tinyblob DEFAULT NULL,
              `context_user_id_bin` tinyblob DEFAULT NULL,
              `context_parent_id_bin` tinyblob DEFAULT NULL,
              `metadata_id` int(20) DEFAULT NULL,
              PRIMARY KEY (`state_id`),
              KEY `ix_states_last_updated_ts` (`last_updated_ts`),
              KEY `ix_states_old_state_id` (`old_state_id`),
              KEY `ix_states_attributes_id` (`attributes_id`),
              KEY `ix_states_context_id_bin` (`context_id_bin`(16)),
              KEY `ix_states_metadata_id_last_updated_ts` (`metadata_id`,`last_updated_ts`),
              CONSTRAINT `states_ibfk_2` FOREIGN KEY (`old_state_id`) REFERENCES `states` (`state_id`)
            ) ENGINE=InnoDB AUTO_INCREMENT=14330350 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """

        private const val CREATE_STATES_META_TABLE_QUERY = """
            CREATE TABLE `states_meta` (
              `metadata_id` int(11) NOT NULL AUTO_INCREMENT,
              `entity_id` varchar(255) DEFAULT NULL,
              PRIMARY KEY (`metadata_id`),
              UNIQUE KEY `ix_states_meta_entity_id` (`entity_id`)
            ) ENGINE=InnoDB AUTO_INCREMENT=251 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """

        fun createMariaDBContainer(): MariaDBContainer<*> = MariaDBContainer("mariadb")
    }
}
