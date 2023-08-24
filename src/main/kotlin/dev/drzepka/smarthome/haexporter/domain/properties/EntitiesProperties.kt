package dev.drzepka.smarthome.haexporter.domain.properties

data class EntitiesProperties(private val properties: List<EntityProperties>) : List<EntityProperties> by properties