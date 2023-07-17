package dev.drzepka.smarthome.haexporter.domain.value

enum class EntityClass {
    SWITCH;

    companion object {
        fun fromString(value: String): EntityClass? = try {
            valueOf(value.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
