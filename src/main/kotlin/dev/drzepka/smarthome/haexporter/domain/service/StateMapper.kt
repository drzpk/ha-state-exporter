package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.exception.MappingException
import dev.drzepka.smarthome.haexporter.domain.value.DefaultValueMapping
import dev.drzepka.smarthome.haexporter.domain.value.StateMapping
import dev.drzepka.smarthome.haexporter.domain.value.StateMappingTargetType
import org.apache.logging.log4j.kotlin.Logging

class StateMapper {
    private val mappings = mutableMapOf<String?, ValueMap>()

    fun registerMapping(stateMapping: StateMapping) {
        logger.info { "Registering mapping: ${stateMapping.name}" }

        val valueMap = mutableMapOf<String?, DefaultValueMapping>()
        var mappingCount = 0

        stateMapping.mappings.forEach {
            val convertedTo = convertToTargetType(it.to, it.toType)
                ?: throwMappingException(stateMapping.name, it.to, it.toType)
            valueMap[it.from] = DefaultValueMapping(convertedTo, it.toType)
            mappingCount++
        }

        stateMapping.defaultMapping?.let {
            val convertedTo = convertToTargetType(it.to, it.toType)
                ?: throwMappingException(stateMapping.name, it.to, it.toType)
            valueMap[null] = it.copy(to = convertedTo)
            mappingCount++
        }

        mappings[stateMapping.name] = valueMap
        logger.info { "Mapping has been registered with $mappingCount value mapping(s)" }
    }

    fun mapState(mappingName: String, input: String): Pair<StateMappingTargetType, Any>? {
        logger.trace { "Converting the input '$input' of mapping '$mappingName" }

        val mapping = mappings[mappingName]
        if (mapping == null) {
            logger.debug { "Mapping '$mappingName' wasn't found" }
            return null
        }

        var result = mapping[input]
        if (result == null)
            result = mapping[null]

        return result?.let { it.toType to it.to }
    }

    private fun convertToTargetType(source: Any, toType: StateMappingTargetType): Any? = when (toType) {
        StateMappingTargetType.STRING -> source
        StateMappingTargetType.LONG -> convertToLong(source)
        StateMappingTargetType.DOUBLE -> convertToDouble(source)
        StateMappingTargetType.BOOL -> convertToBool(source)
    }

    private fun convertToLong(value: Any): Long? = when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }


    private fun convertToDouble(value: Any): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
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

typealias ValueMap = Map<String?, DefaultValueMapping>
