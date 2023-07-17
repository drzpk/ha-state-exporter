package dev.drzepka.smarthome.haexporter.domain.value

data class EntityId(
    val classValue: String,
    val device: String,
    val sensor: String?
) {

    val `class`: EntityClass?
        get() = EntityClass.fromString(classValue)

    override fun toString(): String {
        var str = "$classValue.${device}"
        if (sensor != null)
            str += "_$sensor"
        return str
    }
}
