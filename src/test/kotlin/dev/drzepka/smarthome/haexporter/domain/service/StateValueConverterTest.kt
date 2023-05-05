package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.value.NumericStateValue
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class StateValueConverterTest {

    private val converter = StateValueConverter()

    @Test
    fun `should convert value to INT`() {
        then(converter.convert("178623")).isEqualTo(NumericStateValue(178623))
        then(converter.convert(Int.MIN_VALUE.toString())).isEqualTo(NumericStateValue(Int.MIN_VALUE))
        then(converter.convert(Int.MAX_VALUE.toString())).isEqualTo(NumericStateValue(Int.MAX_VALUE))
    }

    @Test
    fun `should convert value to LONG`() {
        then(converter.convert((Int.MAX_VALUE + 1L).toString())).isEqualTo(NumericStateValue(Int.MAX_VALUE + 1L))
        then(converter.convert((Int.MIN_VALUE - 1L).toString())).isEqualTo(NumericStateValue(Int.MIN_VALUE - 1L))
    }

    @Test
    fun `should convert value to DOUBLE`() {
        then(converter.convert("1.0")).isEqualTo(NumericStateValue(1.0))
        then(converter.convert("3.1123")).isEqualTo(NumericStateValue(3.1123))
    }

    @ParameterizedTest
    @ValueSource(strings = ["string", "12.34.56", "123aab", "12.12oo", "12,34"])
    fun `should not convert unrecognized value`(value: String) {
        then(converter.convert(value)).isNull()
    }
}
