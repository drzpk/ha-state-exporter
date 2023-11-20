package dev.drzepka.smarthome.haexporter.infrastructure.provider

import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.fromDBDateTime
import dev.drzepka.smarthome.haexporter.infrastructure.database.toDBDateTime
import io.r2dbc.spi.Readable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.withContext
import java.time.Instant

@Suppress("SqlNoDataSourceInspection")
class SQLHomeAssistantStateProvider(private val provider: SQLConnectionProvider) : HomeAssistantStateProvider {

    override suspend fun getStates(fromInclusive: Instant, offset: Int, limit: Int): List<SourceState> {
        return withContext(Dispatchers.IO) {
            provider.getConnection()
                .createStatement(
                    """
                    SELECT $COLUMN_ID, $COLUMN_ENTITY_ID, $COLUMN_STATE, $COLUMN_LAST_UPDATED
                    FROM $TABLE_STATES
                    WHERE $COLUMN_LAST_UPDATED >= ${fromInclusive.toDBDateTime()}
                    ORDER BY $COLUMN_LAST_UPDATED
                    LIMIT $offset, $limit
                 """.trimIndent()
                )
                .execute()
                .awaitFirst()
                .map { row -> row.toSourceState() }
                .asFlow()
                .toList(mutableListOf())
        }
    }

    private fun Readable.toSourceState(): SourceState = SourceState(
        (get(COLUMN_ID) as Int).toLong(),
        get(COLUMN_ENTITY_ID) as String,
        get(COLUMN_STATE) as String,
        (get(COLUMN_LAST_UPDATED) as Double).fromDBDateTime()
    )

    companion object {
        private const val TABLE_STATES = "states"
        private const val COLUMN_ID = "state_id"
        private const val COLUMN_ENTITY_ID = "entity_id"
        private const val COLUMN_STATE = "state"
        private const val COLUMN_LAST_UPDATED = "last_updated_ts"
    }
}
