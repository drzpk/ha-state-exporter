package dev.drzepka.smarthome.haexporter.domain.value

sealed class StateValue {
    abstract val value: Any

    override fun equals(other: Any?): Boolean =
        other is StateValue && value == other.value && this::class == other::class

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "${this::class.java.simpleName}($value)"
}

class StringStateValue(override val value: String) : StateValue() {
    val stringValue: String
        get() = value
}

class NumericStateValue(override val value: Number) : StateValue() {
    val numberValue: Number
        get() = value
}

class BooleanStateValue(override val value: Boolean) : StateValue() {
    val booleanValue: Boolean
        get() = value
}
