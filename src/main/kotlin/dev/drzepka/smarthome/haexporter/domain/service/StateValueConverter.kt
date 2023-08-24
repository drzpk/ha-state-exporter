package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.util.Component
import dev.drzepka.smarthome.haexporter.domain.value.NumericStateValue
import dev.drzepka.smarthome.haexporter.domain.value.StateValue

@Component
class StateValueConverter {

    private val conversionOrder = listOf<(String) -> StateValue?>(
        this::convertToDouble,
        this::convertToIntOrLong
    )

    // For now, it's the same
    fun convertToNumber(value: String): StateValue? = convert(value)

    fun convert(value: String): StateValue? {
        for (converter in conversionOrder) {
            val result = converter.invoke(value)
            if (result != null)
                return result
        }

        return null
    }

    private fun convertToDouble(value: String): StateValue? =
        if (value.contains('.'))
            value.toDoubleOrNull()?.let { NumericStateValue(it) }
        else null

    private fun convertToIntOrLong(value: String): StateValue? {
        val number = value.toLongOrNull() ?: return null
        return if (number >= Int.MIN_VALUE && number <= Int.MAX_VALUE)
            NumericStateValue(number.toInt())
        else
            NumericStateValue(number)
    }
}
