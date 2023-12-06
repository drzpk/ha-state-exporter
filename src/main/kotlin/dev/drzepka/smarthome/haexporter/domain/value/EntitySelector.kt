package dev.drzepka.smarthome.haexporter.domain.value

data class EntitySelector(
    val classes: List<String> = listOf(ANY_VALUE),
    val devices: List<String>,
    val sensors: List<String>? = listOf(ANY_VALUE)
) {
    init {
        // Device must be always known because it's used to draw boundary between device and sensor part in entity ID
        if (devices.contains(ANY_VALUE))
            throw IllegalArgumentException("Entity selector can't match all devices")
    }

    constructor(`class`: String, device: String, sensor: String?)
        : this(listOf(`class`), listOf(device), if (sensor != null) listOf(sensor) else null)

    fun toElementalSelectors(): List<ElementalEntitySelector> = classes.flatMap { clazz ->
        devices.flatMap { device ->
            if (!sensors.isNullOrEmpty())
                sensors.map { sensor -> ElementalEntitySelector(clazz, device, sensor) }
            else
                listOf(ElementalEntitySelector(clazz, device, null))
        }
    }

    fun matches(entityId: EntityId): Boolean = entityId.let {
        matchesClass(it.classValue) && matchesDevice(it.device) && matchesSensor(it.sensor)
    }

    private fun matchesClass(clazz: String): Boolean = classes.any { it == clazz || it == ANY_VALUE }

    private fun matchesDevice(device: String): Boolean = devices.any { it == device || it == ANY_VALUE }

    private fun matchesSensor(sensor: String?): Boolean = sensors == null && sensor == null
        || sensors?.any { it == sensor || it == ANY_VALUE } ?: false
}

data class ElementalEntitySelector(val `class`: String, val device: String, val sensor: String?) {
    override fun toString(): String = "class: ${`class`}, device: $device, sensor: $sensor"
}

const val ANY_VALUE = "*"
