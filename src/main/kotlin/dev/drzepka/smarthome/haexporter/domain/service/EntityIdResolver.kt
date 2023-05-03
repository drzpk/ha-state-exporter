package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.domain.properties.DevicesProperties
import dev.drzepka.smarthome.haexporter.domain.value.EntityId
import org.apache.logging.log4j.kotlin.Logging

class EntityIdResolver(private val devicesProperties: DevicesProperties) {

    fun resolve(value: String): EntityId? {
        val parts = value.split('.', limit = 2)
        if (parts.size != 2) {
            logger.debug { "Cannot resolve value: $value, domain delimiter wasn't found" }
            return null
        }

        val pair = resolveDeviceAndSuffix(parts[1])
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

    private fun resolveDeviceAndSuffix(value: String): Pair<String, String?>? {
        val matchedDeviceId = devicesProperties
            .map { it.id }
            .firstOrNull { value == it || value.startsWith("${it}_") }
            ?: return null

        val suffix = if (matchedDeviceId != value)
            value.substringAfter("${matchedDeviceId}_")
        else null

        return matchedDeviceId to suffix
    }

    companion object : Logging
}
