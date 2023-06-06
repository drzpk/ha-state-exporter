package dev.drzepka.smarthome.haexporter.infrasctucture.repository

import dev.drzepka.smarthome.haexporter.domain.entity.SourceState
import dev.drzepka.smarthome.haexporter.domain.repository.SourceStateRepository
import dev.drzepka.smarthome.haexporter.infrasctucture.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrasctucture.database.fromDBDateTime
import dev.drzepka.smarthome.haexporter.infrasctucture.database.toDBDateTime
import io.r2dbc.spi.Readable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import java.time.Instant

@Suppress("SqlNoDataSourceInspection")
class SQLSourceStateRepository(private val provider: SQLConnectionProvider) : SourceStateRepository {

    override suspend fun getStates(fromInclusive: Instant, limit: Int): Flow<SourceState> {
        return provider.getConnection()
            .createStatement(
                """
                    SELECT $COLUMN_ID, $COLUMN_ENTITY_ID, $COLUMN_STATE, $COLUMN_LAST_UPDATED
                    FROM $TABLE_STATES
                    WHERE $COLUMN_LAST_UPDATED >= ${fromInclusive.toDBDateTime()}
                    ORDER BY $COLUMN_LAST_UPDATED
                    LIMIT $limit
                 """.trimIndent()
            )
            .execute()
            .awaitSingle()
            .map { row -> row.toSourceState() }
            .asFlow()
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
