package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.exception.MappingException
import dev.drzepka.smarthome.haexporter.domain.value.*
import org.assertj.core.api.BDDAssertions.catchException
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class StateMapperTest {

    @Test
    fun `should map state to STRING`() {
        val mapping = StateMapping(
            "string-mapping",
            listOf(
                ValueMapping("disabled", "off", StateMappingTargetType.STRING),
                ValueMapping("enabled", "on", StateMappingTargetType.STRING),
            ),
            null
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output1 = mapper.mapState("string-mapping", "enabled")
        val output2 = mapper.mapState("string-mapping", "disabled")

        then(output1).isEqualTo(StringStateValue("on"))
        then(output2).isEqualTo(StringStateValue("off"))
    }

    @Test
    fun `should map state to NUMBER (integer)`() {
        val mapping = StateMapping(
            "int-mapping",
            listOf(
                ValueMapping("small", 1, StateMappingTargetType.NUMBER),
                ValueMapping("medium", 2, StateMappingTargetType.NUMBER),
                ValueMapping("large", 3, StateMappingTargetType.NUMBER),
                ValueMapping("xl", "4", StateMappingTargetType.NUMBER),
            ),
            null
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output1 = mapper.mapState("int-mapping", "small")
        val output2 = mapper.mapState("int-mapping", "medium")
        val output3 = mapper.mapState("int-mapping", "large")
        val output4 = mapper.mapState("int-mapping", "xl")

        then(output1).isEqualTo(NumericStateValue(1))
        then(output2).isEqualTo(NumericStateValue(2))
        then(output3).isEqualTo(NumericStateValue(3))
        then(output4).isEqualTo(NumericStateValue(4))
    }

    @Test
    fun `should map state to NUMBER (long)`() {
        val mapping = StateMapping(
            "long-mapping",
            listOf(
                ValueMapping("small", Int.MAX_VALUE + 1L, StateMappingTargetType.NUMBER),
                ValueMapping("medium", (Int.MIN_VALUE - 5L).toString(), StateMappingTargetType.NUMBER),
            ),
            null
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output1 = mapper.mapState("long-mapping", "small")
        val output2 = mapper.mapState("long-mapping", "medium")

        then(output1).isEqualTo(NumericStateValue(Int.MAX_VALUE + 1L))
        then(output2).isEqualTo(NumericStateValue(Int.MIN_VALUE - 5L))
    }

    @Test
    fun `should map state to NUMBER (double`() {
        val mapping = StateMapping(
            "double-mapping",
            listOf(
                ValueMapping("large", 3.0, StateMappingTargetType.NUMBER),
                ValueMapping("xl", "4.123", StateMappingTargetType.NUMBER),
            ),
            null
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output1 = mapper.mapState("double-mapping", "large")
        val output2 = mapper.mapState("double-mapping", "xl")

        then(output1).isEqualTo(NumericStateValue(3.0))
        then(output2).isEqualTo(NumericStateValue(4.123))
    }

    @Test
    fun `should map state to BOOL`() {
        val mapping = StateMapping(
            "bool-mapping",
            listOf(
                ValueMapping("off", false, StateMappingTargetType.BOOL),
                ValueMapping("on", true, StateMappingTargetType.BOOL),
                ValueMapping("enabled", 2, StateMappingTargetType.BOOL),
            ),
            null
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output1 = mapper.mapState("bool-mapping", "off")
        val output2 = mapper.mapState("bool-mapping", "on")
        val output3 = mapper.mapState("bool-mapping", "enabled")

        then(output1).isEqualTo(BooleanStateValue(false))
        then(output2).isEqualTo(BooleanStateValue(true))
        then(output3).isEqualTo(BooleanStateValue(true))
    }

    @Test
    fun `should use default mapping if none was matched`() {
        val mapping = StateMapping(
            "mapping",
            listOf(
                ValueMapping("disabled", 0, StateMappingTargetType.NUMBER),
                ValueMapping("enabled", 1, StateMappingTargetType.NUMBER),
            ),
            DefaultValueMapping(-1, StateMappingTargetType.NUMBER)
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output = mapper.mapState("mapping", "unknown")
        then(output).isEqualTo(NumericStateValue(-1))
    }

    @Test
    fun `should return null when no mapping was matched and there is no default one`() {
        val mapping = StateMapping(
            "mapping",
            listOf(
                ValueMapping("disabled", 0, StateMappingTargetType.NUMBER),
                ValueMapping("enabled", 1, StateMappingTargetType.NUMBER),
            ),
            null
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output = mapper.mapState("mapping", "unknown")
        then(output).isNull()
    }

    @ParameterizedTest
    @MethodSource("getInvalidMappings")
    fun `should throw exception on target type mismatch`(valueMapping: ValueMapping) {
        val mapping = StateMapping("mapping", listOf(valueMapping), null)
        val mapper = StateMapper()

        val exception = catchException { mapper.registerMapping(mapping) }

        then(exception).isInstanceOf(MappingException::class.java)
        then(exception.message).contains("Value '${valueMapping.to}' cannot be represented as type ${valueMapping.toType}")
    }

    companion object {
        @JvmStatic
        private fun getInvalidMappings(): Stream<ValueMapping> = listOf(
            ValueMapping("test1", "abc", StateMappingTargetType.NUMBER),
            ValueMapping("test2", "xyz", StateMappingTargetType.NUMBER),
            ValueMapping("test3", "jkl", StateMappingTargetType.BOOL),
        ).stream()
    }
}
