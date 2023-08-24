package dev.drzepka.smarthome.haexporter.domain.properties

import dev.drzepka.smarthome.haexporter.domain.value.EntitySelector

data class EntityProperties(
    val selector: EntitySelector,
    val schema: String
)
