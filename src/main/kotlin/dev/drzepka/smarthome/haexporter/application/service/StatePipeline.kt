package dev.drzepka.smarthome.haexporter.application.service

import dev.drzepka.smarthome.haexporter.application.model.SourceState
import dev.drzepka.smarthome.haexporter.application.properties.EntitySchema
import dev.drzepka.smarthome.haexporter.application.properties.SchemaProperties
import dev.drzepka.smarthome.haexporter.application.properties.SchemasProperties
import dev.drzepka.smarthome.haexporter.domain.entity.State
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.service.EntityConfigurationResolver
import dev.drzepka.smarthome.haexporter.domain.service.EntityIdResolver
import dev.drzepka.smarthome.haexporter.domain.service.StateMapper
import dev.drzepka.smarthome.haexporter.domain.service.StateValueConverter
import dev.drzepka.smarthome.haexporter.domain.util.Component
import dev.drzepka.smarthome.haexporter.domain.value.StateValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.apache.logging.log4j.kotlin.Logging

@Component
class StatePipeline(
    private val entityIdResolver: EntityIdResolver,
    private val entityConfigurationResolver: EntityConfigurationResolver,
    private val stateMapper: StateMapper,
    private val stateValueConverter: StateValueConverter,
    private val schemas: SchemasProperties,
    private val stateRepository: StateRepository
) {

    suspend fun execute(source: Flow<SourceState>) {
        val mapped = source.mapNotNull {
            try {
                processSingle(it)
            } catch (e: Exception) {
                logger.error(e) { "Error while processing $it" }
                null
            }
        }

        stateRepository.save(mapped)
    }

    private fun processSingle(input: SourceState): State? {
        logger.trace { "Processing $input" }

        val entityId = entityIdResolver.resolve(input.entityId)
        if (entityId == null) {
            logger.debug { "Couldn't resolve entity with id=${input.entityId}" }
            return null
        }

        val configuration = entityConfigurationResolver.resolve(entityId)
        if (configuration == null) {
            logger.debug { "Couldn't resolve configuration for $entityId" }
            return null
        } else {
            logger.trace { "Resolved configuration for entity $entityId: $configuration" }
        }

        val schema = getSchema(configuration.schema)
        if (schema == null) {
            logger.debug { "Couldn't resolve schema '${configuration.schema}'" }
            return null
        }

        val value = processState(input.state, schema.getEntitySchema(entityId.sensor))
        return value?.let { State(input.time, entityId, schema.influxMeasurementName, it) }
    }

    private fun processState(input: String, schema: EntitySchema): StateValue? {
        var value = if (schema.stateMapping != null) {
            val result = stateMapper.mapState(schema.stateMapping, input)
            logger.trace { "Used mapping ${schema.stateMapping} to map value '$input' to $result" }
            result
        } else null

        if (schema.stateMapping == null || value == null && !schema.ignoreUnmappedState)
            value = stateValueConverter.convert(input, schema.type)

        return value
    }

    private fun getSchema(name: String): SchemaProperties? = schemas.firstOrNull { it.name == name }

    companion object : Logging
}
