package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.properties.EntityProperties
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import org.apache.logging.log4j.kotlin.Logging

class EntityIdResolver(entitiesProperties: List<EntityProperties>) {
    private val knownDevices = entitiesProperties.map { it.selector.device }.toSet()

    fun resolve(value: String): EntityId? {
        val parts = value.split('.', limit = 2)
        if (parts.size != 2) {
            logger.debug { "Cannot resolve value: $value, class delimiter wasn't found" }
            return null
        }

        val pair = resolveDeviceAndSensor(parts[1])
        if (pair == null) {
            logger.debug { "Cannot resolve value: $value, device not found in configuration" }
            return null
        }

        return EntityId(
            parts[0],
            pair.first,
            pair.second
        )
    }

    private fun resolveDeviceAndSensor(value: String): Pair<String, String?>? {
        val matchedDeviceId = knownDevices
            .firstOrNull { value == it || value.startsWith("${it}_") }
            ?: return null

        val sensor = if (matchedDeviceId != value)
            value.substringAfter("${matchedDeviceId}_")
        else null

        return matchedDeviceId to sensor
    }

    companion object : Logging
}
