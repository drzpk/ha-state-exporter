package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.value.DoubleStateValue
import dev.drzepka.smarthome.haexporter.domain.value.LongStateValue
import dev.drzepka.smarthome.haexporter.domain.value.StringStateValue
import dev.drzepka.smarthome.haexporter.domain.value.ValueType
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class StateValueConverterTest {

    private val converter = StateValueConverter()

    @Test
    fun `should convert value to STRING`() {
        val payload = "test string"
        then(converter.convert(payload, ValueType.STRING)).isEqualTo(StringStateValue(payload))
    }

    @Test
    fun `should convert value to INTEGER`() {
        then(converter.convert("178623", ValueType.INTEGER)).isEqualTo(LongStateValue(178623))
        then(converter.convert(Long.MIN_VALUE.toString(), ValueType.INTEGER)).isEqualTo(LongStateValue(Long.MIN_VALUE))
        then(converter.convert(Long.MAX_VALUE.toString(), ValueType.INTEGER)).isEqualTo(LongStateValue(Long.MAX_VALUE))

        val overflowMax = BigDecimal(Long.MAX_VALUE) + BigDecimal.ONE
        then(converter.convert(overflowMax.toString(), ValueType.INTEGER)).isNull()

        val overflowMin = BigDecimal(Long.MIN_VALUE) - BigDecimal.ONE
        then(converter.convert(overflowMin.toString(), ValueType.INTEGER)).isNull()
    }

    @Test
    fun `should convert value to DOUBLE`() {
        then(converter.convert("1.0", ValueType.FLOAT)).isEqualTo(DoubleStateValue(1.0))
        then(converter.convert("3.1123", ValueType.FLOAT)).isEqualTo(DoubleStateValue(3.1123))
        then(converter.convert("3.1123", ValueType.FLOAT)).isEqualTo(DoubleStateValue(3.1123))
        then(converter.convert("2.11111111111111111111111111", ValueType.FLOAT)).isEqualTo(DoubleStateValue(2.111111111111111))
        then(converter.convert("abc", ValueType.FLOAT)).isNull()

    }
}
