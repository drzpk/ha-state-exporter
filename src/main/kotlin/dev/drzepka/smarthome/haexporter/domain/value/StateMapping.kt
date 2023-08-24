package dev.drzepka.smarthome.haexporter.domain.value

class StateMappings(private val mappings: List<StateMapping>) : List<StateMapping> by mappings

data class StateMapping(
    val name: String,
    val mappings: List<ValueMapping>,
    val defaultMapping: DefaultValueMapping?
)

data class ValueMapping(
    val from: String,
    val to: Any,
    val toType: StateMappingTargetType = StateMappingTargetType.STRING,
)

data class DefaultValueMapping(
    val to: Any,
    val toType: StateMappingTargetType = StateMappingTargetType.STRING
)

enum class StateMappingTargetType {
    STRING, NUMBER, BOOL
}
