package dev.drzepka.smarthome.haexporter.domain.properties

class DevicesProperties : ArrayList<DeviceProperties>() {
    fun findById(id: String): DeviceProperties? = this.find { it.id == id }
}

data class DeviceProperties(
    val id: String,
    val schema: String
)
