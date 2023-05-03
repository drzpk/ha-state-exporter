package dev.drzepka.smarthome.haexporter.domain.value

enum class Domain {
    SWITCH;

    companion object {
        fun fromString(value: String): Domain? = try {
            valueOf(value.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
