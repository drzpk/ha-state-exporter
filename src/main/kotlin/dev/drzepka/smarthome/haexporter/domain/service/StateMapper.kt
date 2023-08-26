package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.exception.MappingException
import dev.drzepka.smarthome.haexporter.domain.value.StateMapping
import dev.drzepka.smarthome.haexporter.domain.value.StateMappings
import dev.drzepka.smarthome.haexporter.domain.value.StateValue
import dev.drzepka.smarthome.haexporter.domain.value.ValueType
import org.apache.logging.log4j.kotlin.Logging

class StateMapper(converter: StateValueConverter, stateMappings: StateMappings) {
    private val mappings = mutableMapOf<String?, ValueMap>()

    init {
        stateMappings.forEach { registerMapping(converter, it) }
        logger.info { "Processed ${stateMappings.size} state mappings" }
    }

    fun mapState(mappingName: String, input: String): StateValue? {
        logger.trace { "Converting the input '$input' of mapping '$mappingName" }

        val mapping = mappings[mappingName]
        if (mapping == null) {
            logger.debug { "Mapping '$mappingName' wasn't found" }
            return null
        }

        var result = mapping[input]
        if (result == null)
            result = mapping[null]

        return result
    }

    private fun registerMapping(converter: StateValueConverter, stateMapping: StateMapping) {
        logger.info { "Registering mapping: ${stateMapping.name}" }

        val valueMap = mutableMapOf<String?, StateValue>()
        var mappingCount = 0

        stateMapping.mappings.forEach {
            val stateValue = converter.convert(it.to, stateMapping.targetType)
                ?: throwMappingException(stateMapping.name, it.to, stateMapping.targetType)
            valueMap[it.from] = stateValue
            mappingCount++
        }

        stateMapping.defaultMapping?.let {
            val stateValue = converter.convert(it.to, stateMapping.targetType)
                ?: throwMappingException(stateMapping.name, it.to, stateMapping.targetType)
            valueMap[null] = stateValue
            mappingCount++
        }

        mappings[stateMapping.name] = valueMap
        logger.info { "Mapping ${stateMapping.name} has been registered with $mappingCount value mapping(s)" }
    }

    private fun throwMappingException(mappingName: String, value: Any, targetType: ValueType): Nothing =
        throw MappingException("Unable to register state mapping '$mappingName'. Value '$value' cannot be represented as type $targetType")

    companion object : Logging
}

private typealias ValueMap = Map<String?, StateValue>
