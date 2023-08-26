package dev.drzepka.smarthome.haexporter.domain.value

class StateMappings(private val mappings: List<StateMapping>) : List<StateMapping> by mappings

data class StateMapping(
    val name: String,
    val targetType: ValueType,
    val mappings: List<ValueMapping>,
    val defaultMapping: DefaultValueMapping?
)

data class ValueMapping(val from: String, val to: String)

data class DefaultValueMapping(val to: String)
