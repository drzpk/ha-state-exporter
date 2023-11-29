package dev.drzepka.smarthome.haexporter.infrastructure.provider

import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.model.SourceStateQuery
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.fromDBDateTime
import dev.drzepka.smarthome.haexporter.infrastructure.database.toDBDateTime
import org.apache.logging.log4j.kotlin.Logging
import java.sql.ResultSet

@Suppress("SqlNoDataSourceInspection")
class SQLHomeAssistantStateProvider(private val provider: SQLConnectionProvider) : HomeAssistantStateProvider {

    override suspend fun getStates(query: SourceStateQuery): List<SourceState> {
        val sqlQuery = createSqlQuery(query)
        return provider.acquireConnection {
            val resultSet = it
                .createStatement()
                .executeQueryAsync(sqlQuery)
            val states = ArrayList<SourceState>(query.limit)
            while (resultSet.next()) {
                convertToSourceState(resultSet)?.also { state -> states.add(state) }
            }

            resultSet.closeAsync()
            states
        }
    }

    private suspend fun createSqlQuery(query: SourceStateQuery): String {
        val metadataIdsCondition = if (query.entities?.isNotEmpty() == true)
            getMetadataIdsCondition(query.entities)
                .joinToString(prefix = "(", separator = ", ", postfix = ")")
                .let { "$COLUMN_METADATA_ID IN $it" }
        else "1=1"

        return """
            SELECT s.*, m.$COLUMN_ENTITY_ID
            FROM $TABLE_STATES_META m
            JOIN (
                SELECT $COLUMN_ID, $COLUMN_STATE, $COLUMN_LAST_UPDATED, $COLUMN_METADATA_ID
                FROM $TABLE_STATES
                WHERE $COLUMN_LAST_UPDATED >= ${query.from.toDBDateTime()} AND $metadataIdsCondition
                LIMIT ${query.offset}, ${query.limit}
            ) s
            ON s.$COLUMN_METADATA_ID = m.$COLUMN_METADATA_ID
            ORDER BY s.$COLUMN_LAST_UPDATED
        """.trimIndent()
    }

    private suspend fun getMetadataIdsCondition(entities: Collection<String>): Set<Int> =
        provider.acquireConnection {
            val entityIds = entities.joinToString { e ->"'" + e.replace("'", "''") + "'" }
            val rs = it
                .createStatement()
                .executeQueryAsync("""
                    SELECT $COLUMN_METADATA_ID
                    FROM $TABLE_STATES_META
                    WHERE entity_id IN ($entityIds)
                """.trimIndent())

            sequence {
                while (rs.next()) {
                    val id = rs.getInt(COLUMN_METADATA_ID)
                    if (!rs.wasNull())
                        yield(id)
                }
            }
        }.toSet()

    private fun convertToSourceState(source: ResultSet): SourceState? = try {
        doConvertToSourceState(source)
    } catch (e: Exception) {
        logger.error("Error creating source state from result set", e)
        null
    }

    private fun doConvertToSourceState(source: ResultSet): SourceState? {
        val id = source.getLong(COLUMN_ID)

        val entityId = source.getString(COLUMN_ENTITY_ID)
        if (entityId == null) {
            logger.warn { "Skipping state with id=$id, $COLUMN_ENTITY_ID is null" }
            return null
        }

        val state = source.getString(COLUMN_STATE)
        if (state == null) {
            logger.warn { "Skipping state with id=$id, $COLUMN_STATE is null" }
            return null
        }

        val lastUpdated = source.getDouble(COLUMN_LAST_UPDATED)
        if (source.wasNull()) {
            logger.warn { "Skipping state with id=$id, $COLUMN_LAST_UPDATED is null" }
            return null
        }


        return SourceState(
            id,
            entityId,
            state,
            lastUpdated.fromDBDateTime()
        )
    }

    companion object : Logging {
        private const val TABLE_STATES = "states"
        private const val TABLE_STATES_META = "states_meta"
        private const val COLUMN_ID = "state_id"
        private const val COLUMN_ENTITY_ID = "entity_id"
        private const val COLUMN_STATE = "state"
        private const val COLUMN_LAST_UPDATED = "last_updated_ts"
        private const val COLUMN_METADATA_ID = "metadata_id"
    }
}
