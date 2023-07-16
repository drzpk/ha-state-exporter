package dev.drzepka.smarthome.haexporter.domain.properties

import dev.drzepka.smarthome.haexporter.domain.value.EntitySelector

class EntitiesProperties(vararg entityProperties: EntityProperties) : ArrayList<EntityProperties>(entityProperties.toList())

data class EntityProperties(
    val selector: EntitySelector,
    val mapping: String? = null
)
