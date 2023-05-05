package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.exception.MappingException
import dev.drzepka.smarthome.haexporter.domain.value.*
import org.apache.logging.log4j.kotlin.Logging

class StateMapper {
    private val mappings = mutableMapOf<String?, ValueMap>()

    fun registerMapping(stateMapping: StateMapping) {
        logger.info { "Registering mapping: ${stateMapping.name}" }

        val valueMap = mutableMapOf<String?, StateValue>()
        var mappingCount = 0

        stateMapping.mappings.forEach {
            val stateValue = convertToStateValue(it.to, it.toType)
                ?: throwMappingException(stateMapping.name, it.to, it.toType)
            valueMap[it.from] = stateValue
            mappingCount++
        }

        stateMapping.defaultMapping?.let {
            val stateValue = convertToStateValue(it.to, it.toType)
                ?: throwMappingException(stateMapping.name, it.to, it.toType)
            valueMap[null] = stateValue
            mappingCount++
        }

        mappings[stateMapping.name] = valueMap
        logger.info { "Mapping has been registered with $mappingCount value mapping(s)" }
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

    private fun convertToStateValue(source: Any, toType: StateMappingTargetType): StateValue? = when (toType) {
        StateMappingTargetType.STRING -> StringStateValue(source as String)
        StateMappingTargetType.NUMBER -> convertToNumber(source)?.let { NumericStateValue(it) }
        StateMappingTargetType.BOOL -> convertToBool(source)?.let { BooleanStateValue(it) }
    }

    private fun convertToNumber(value: Any): Number? {
        return if (value is Number)
            value
        else if (value is String && value.contains('.'))
            value.toDoubleOrNull()
        else if (value is String) {
            val long = value.toLongOrNull() ?: return null
            if (long <= Int.MAX_VALUE && long >= Int.MIN_VALUE)
                long.toInt()
            else
                long
        } else
            null
    }

    private fun convertToBool(value: Any): Boolean? {
        if (value is Boolean)
            return value
        if (value is Number)
            return value.toInt() > 0

        if (value is String) {
            if (value.lowercase() in BOOLEAN_TRUE_VALUES)
                return true
            else if (value.lowercase() in BOOLEAN_FALSE_VALUES)
                return false
        }

        return null
    }

    private fun throwMappingException(mappingName: String, value: Any, targetType: StateMappingTargetType): Nothing =
        throw MappingException("Unable to register state mapping '$mappingName'. Value '$value' cannot be represented as type $targetType")

    companion object : Logging {
        private val BOOLEAN_TRUE_VALUES = setOf("yes", "on", "enabled")
        private val BOOLEAN_FALSE_VALUES = setOf("no", "off", "disabled")
    }
}

typealias ValueMap = Map<String?, StateValue>
