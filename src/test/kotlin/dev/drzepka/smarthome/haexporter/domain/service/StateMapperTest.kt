package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.exception.MappingException
import dev.drzepka.smarthome.haexporter.domain.value.DefaultValueMapping
import dev.drzepka.smarthome.haexporter.domain.value.LongStateValue
import dev.drzepka.smarthome.haexporter.domain.value.StateMapping
import dev.drzepka.smarthome.haexporter.domain.value.StateMappings
import dev.drzepka.smarthome.haexporter.domain.value.ValueMapping
import dev.drzepka.smarthome.haexporter.domain.value.ValueType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.BDDAssertions.assertThatExceptionOfType
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

internal class StateMapperTest {
    private val converter = mockk<StateValueConverter>()

    @Test
    fun `should map state to target type`() {
        val valueMappings = listOf(
            ValueMapping("disabled", "0"),
            ValueMapping("enabled", "1")
        )

        every { converter.convert("0", ValueType.INTEGER) } returns LongStateValue(0L)
        every { converter.convert("1", ValueType.INTEGER) } returns LongStateValue(1L)

        val mapping = StateMapping("mapping_test", ValueType.INTEGER, valueMappings, null)
        val mapper = StateMapper(converter, StateMappings(listOf(mapping)))

        then(mapper.mapState("mapping_test", "disabled")).isEqualTo(LongStateValue(0L))
        then(mapper.mapState("mapping_test", "enabled")).isEqualTo(LongStateValue(1L))

        then(mapper.mapState("mapping_test", " enabled ")).isNull()
        then(mapper.mapState("mapping_test", "something else")).isNull()
        then(mapper.mapState("another mapping", "enabled")).isNull()
    }

    @Test
    fun `should use default mapping if none of the predefined mappings was matched`() {
        val valueMappings = listOf(
            ValueMapping("disabled", "0"),
            ValueMapping("enabled", "1")
        )
        val default = DefaultValueMapping("-1")

        every { converter.convert("0", ValueType.INTEGER) } returns LongStateValue(0L)
        every { converter.convert("1", ValueType.INTEGER) } returns LongStateValue(1L)
        every { converter.convert("-1", ValueType.INTEGER) } returns LongStateValue(-1L)

        val mapping = StateMapping("mapping_test", ValueType.INTEGER, valueMappings, default)
        val mapper = StateMapper(converter, StateMappings(listOf(mapping)))

        then(mapper.mapState("mapping_test", "unknown value")).isEqualTo(LongStateValue(-1L))
        then(mapper.mapState("abc", "unknown value")).isNull()
    }

    @Test
    fun `should throw exception if mappings are invalid`() {
        val valueMappings = listOf(
            ValueMapping("off", "0"),
            ValueMapping("on", "invalid")
        )
        val default = DefaultValueMapping("test")

        every { converter.convert(any(), ValueType.INTEGER) } returns null
        every { converter.convert("0", ValueType.INTEGER) } returns LongStateValue(0L)

        assertThatExceptionOfType(MappingException::class.java)
            .isThrownBy {
                StateMapper(converter, StateMappings(listOf(StateMapping("test", ValueType.INTEGER, valueMappings, null))))
            }
            .withMessageContaining("Value 'invalid' cannot be represented as type INTEGER")

        assertThatExceptionOfType(MappingException::class.java)
            .isThrownBy {
                StateMapper(converter, StateMappings(listOf(StateMapping("test", ValueType.INTEGER, emptyList(), default))))
            }
            .withMessageContaining("Value 'test' cannot be represented as type INTEGER")
    }
}
