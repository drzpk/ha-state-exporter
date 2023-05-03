package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.exception.MappingException
import dev.drzepka.smarthome.haexporter.domain.value.DefaultValueMapping
import dev.drzepka.smarthome.haexporter.domain.value.StateMapping
import dev.drzepka.smarthome.haexporter.domain.value.StateMappingTargetType
import dev.drzepka.smarthome.haexporter.domain.value.ValueMapping
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

        then(output1).isEqualTo(StateMappingTargetType.STRING to "on")
        then(output2).isEqualTo(StateMappingTargetType.STRING to "off")
    }

    @Test
    fun `should map state to LONG`() {
        val mapping = StateMapping(
            "long-mapping",
            listOf(
                ValueMapping("small", 1L, StateMappingTargetType.LONG),
                ValueMapping("medium", 2, StateMappingTargetType.LONG),
                ValueMapping("large", 3.0, StateMappingTargetType.LONG),
                ValueMapping("xl", "4", StateMappingTargetType.LONG),
            ),
            null
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output1 = mapper.mapState("long-mapping", "small")
        val output2 = mapper.mapState("long-mapping", "medium")
        val output3 = mapper.mapState("long-mapping", "large")
        val output4 = mapper.mapState("long-mapping", "xl")

        then(output1).isEqualTo(StateMappingTargetType.LONG to 1L)
        then(output2).isEqualTo(StateMappingTargetType.LONG to 2L)
        then(output3).isEqualTo(StateMappingTargetType.LONG to 3L)
        then(output4).isEqualTo(StateMappingTargetType.LONG to 4L)
    }

    @Test
    fun `should map state to DOUBLE`() {
        val mapping = StateMapping(
            "double-mapping",
            listOf(
                ValueMapping("small", 1L, StateMappingTargetType.DOUBLE),
                ValueMapping("medium", 2, StateMappingTargetType.DOUBLE),
                ValueMapping("large", 3.0, StateMappingTargetType.DOUBLE),
                ValueMapping("xl", "4", StateMappingTargetType.DOUBLE),
            ),
            null
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output1 = mapper.mapState("double-mapping", "small")
        val output2 = mapper.mapState("double-mapping", "medium")
        val output3 = mapper.mapState("double-mapping", "large")
        val output4 = mapper.mapState("double-mapping", "xl")

        then(output1).isEqualTo(StateMappingTargetType.DOUBLE to 1.0)
        then(output2).isEqualTo(StateMappingTargetType.DOUBLE to 2.0)
        then(output3).isEqualTo(StateMappingTargetType.DOUBLE to 3.0)
        then(output4).isEqualTo(StateMappingTargetType.DOUBLE to 4.0)
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

        then(output1).isEqualTo(StateMappingTargetType.BOOL to false)
        then(output2).isEqualTo(StateMappingTargetType.BOOL to true)
        then(output3).isEqualTo(StateMappingTargetType.BOOL to true)
    }

    @Test
    fun `should use default mapping if none was matched`() {
        val mapping = StateMapping(
            "mapping",
            listOf(
                ValueMapping("disabled", 0, StateMappingTargetType.LONG),
                ValueMapping("enabled", 1, StateMappingTargetType.LONG),
            ),
            DefaultValueMapping(-1, StateMappingTargetType.LONG)
        )
        val mapper = StateMapper().apply { registerMapping(mapping) }

        val output = mapper.mapState("mapping", "unknown")
        then(output).isEqualTo(StateMappingTargetType.LONG to -1L)
    }

    @Test
    fun `should return null when no mapping was matched and there is no default one`() {
        val mapping = StateMapping(
            "mapping",
            listOf(
                ValueMapping("disabled", 0, StateMappingTargetType.LONG),
                ValueMapping("enabled", 1, StateMappingTargetType.LONG),
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
            ValueMapping("test1", "abc", StateMappingTargetType.LONG),
            ValueMapping("test2", "xyz", StateMappingTargetType.DOUBLE),
            ValueMapping("test3", "jkl", StateMappingTargetType.BOOL),
        ).stream()
    }
}
