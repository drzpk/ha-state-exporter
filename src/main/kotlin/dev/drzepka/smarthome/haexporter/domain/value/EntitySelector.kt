package dev.drzepka.smarthome.haexporter.domain.value

data class EntitySelector(
    val classes: List<String> = listOf(ANY_VALUE),
    val device: String,
    val sensors: List<String>? = listOf(ANY_VALUE)
) {
    constructor(`class`: String, device: String, sensor: String?)
        : this(listOf(`class`), device, if (sensor != null) listOf(sensor) else null)

    fun toElementalSelectors(): List<ElementalEntitySelector> = classes.flatMap { clazz ->
        if (!sensors.isNullOrEmpty())
            sensors.map { sensor -> ElementalEntitySelector(clazz, device, sensor) }
        else
            listOf(ElementalEntitySelector(clazz, device, null))
    }

    fun matches(entityId: EntityId): Boolean = entityId.let {
        matchesClass(it.classValue) && matchesDevice(it.device) && matchesSensor(it.sensor)
    }

    private fun matchesClass(clazz: String): Boolean = classes.any { it == clazz || it == ANY_VALUE }

    private fun matchesDevice(device: String): Boolean = this.device == device

    private fun matchesSensor(sensor: String?): Boolean = sensors == null && sensor == null
        || sensors?.any { it == sensor || it == ANY_VALUE } ?: false
}

data class ElementalEntitySelector(val `class`: String, val device: String, val sensor: String?) {
    override fun toString(): String = "class: ${`class`}, device: $device, sensor: $sensor"
}

const val ANY_VALUE = "*"
