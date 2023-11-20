package dev.drzepka.smarthome.haexporter.infrastructure.provider

import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.fromDBDateTime
import dev.drzepka.smarthome.haexporter.infrastructure.database.toDBDateTime
import java.sql.ResultSet
import java.time.Instant

@Suppress("SqlNoDataSourceInspection")
class SQLHomeAssistantStateProvider(private val provider: SQLConnectionProvider) : HomeAssistantStateProvider {

    override suspend fun getStates(fromInclusive: Instant, offset: Int, limit: Int): List<SourceState> {
        val resultSet = provider.getConnection()
            .createStatement()
            .executeQueryAsync(
                """
                    SELECT $COLUMN_ID, $COLUMN_ENTITY_ID, $COLUMN_STATE, $COLUMN_LAST_UPDATED
                    FROM $TABLE_STATES
                    WHERE $COLUMN_LAST_UPDATED >= ${fromInclusive.toDBDateTime()}
                    ORDER BY $COLUMN_LAST_UPDATED
                    LIMIT $offset, $limit
                 """.trimIndent()
            )

        val states = ArrayList<SourceState>(limit)
        while (resultSet.next()) {
            states.add(resultSet.toSourceState())
        }

        return states
    }

    private fun ResultSet.toSourceState(): SourceState = SourceState(
        getLong(COLUMN_ID),
        getString(COLUMN_ENTITY_ID),
        getString(COLUMN_STATE),
        getDouble(COLUMN_LAST_UPDATED).fromDBDateTime()
    )

    companion object {
        private const val TABLE_STATES = "states"
        private const val COLUMN_ID = "state_id"
        private const val COLUMN_ENTITY_ID = "entity_id"
        private const val COLUMN_STATE = "state"
        private const val COLUMN_LAST_UPDATED = "last_updated_ts"
    }
}
