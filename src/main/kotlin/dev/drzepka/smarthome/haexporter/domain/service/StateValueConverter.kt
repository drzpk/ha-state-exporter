package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.util.Component
import dev.drzepka.smarthome.haexporter.domain.value.BooleanStateValue
import dev.drzepka.smarthome.haexporter.domain.value.DoubleStateValue
import dev.drzepka.smarthome.haexporter.domain.value.LongStateValue
import dev.drzepka.smarthome.haexporter.domain.value.StateValue
import dev.drzepka.smarthome.haexporter.domain.value.StringStateValue
import dev.drzepka.smarthome.haexporter.domain.value.ValueType
import org.apache.logging.log4j.kotlin.Logging

@Component
class StateValueConverter {

    fun convert(value: String, targetType: ValueType): StateValue? {
        val converted = when (targetType) {
            ValueType.STRING -> StringStateValue(value)
            ValueType.INTEGER -> convertToLong(value)
            ValueType.FLOAT -> convertToDouble(value)
            ValueType.BOOLEAN -> convertToBoolean(value)
        }

        if (converted != null)
            logger.trace { "Converted value '$value' to $converted" }
        else
            logger.debug { "Unable to convert value '$value' to type $targetType" }

        return converted
    }

    private fun convertToLong(value: String): StateValue? = value.toLongOrNull()?.let { LongStateValue(it) }

    private fun convertToDouble(value: String): StateValue? = value.toDoubleOrNull()?.let { DoubleStateValue(it) }

    private fun convertToBoolean(value: String): StateValue? {
        val sanitized = value.trim().lowercase()
        if (sanitized == TRUE_VALUE)
            return BooleanStateValue(true)
        else if (sanitized == FALSE_VALUE)
            return BooleanStateValue(false)

        val number = value.toDoubleOrNull()
        if (number != null)
            return BooleanStateValue(number > 0.0)

        return null
    }

    companion object : Logging {
        private const val TRUE_VALUE = "true"
        private const val FALSE_VALUE = "false"
    }
}
