package dev.drzepka.smarthome.haexporter.domain.value

sealed class StateValue {
    abstract val value: Any

    abstract fun asString(): String

    override fun equals(other: Any?): Boolean =
        other is StateValue && value == other.value && this::class == other::class

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "${this::class.java.simpleName}($value)"
}

class StringStateValue(override val value: String) : StateValue() {
    val stringValue: String
        get() = value

    override fun asString(): String = stringValue
}

class NumericStateValue(override val value: Number) : StateValue() {
    val numberValue: Number
        get() = value

    override fun asString(): String = numberValue.toString()
}

class BooleanStateValue(override val value: Boolean) : StateValue() {
    val booleanValue: Boolean
        get() = value

    override fun asString(): String = if (booleanValue) "true" else "false"
}
