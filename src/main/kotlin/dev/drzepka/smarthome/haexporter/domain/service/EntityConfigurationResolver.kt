package dev.drzepka.smarthome.haexporter.domain.service

import dev.drzepka.smarthome.haexporter.application.DuplicatedEntitySelectorsException
import dev.drzepka.smarthome.haexporter.domain.properties.EntitiesProperties
import dev.drzepka.smarthome.haexporter.domain.properties.EntityProperties
import dev.drzepka.smarthome.haexporter.domain.value.EntityConfiguration
import dev.drzepka.smarthome.haexporter.domain.value.EntityId

class EntityConfigurationResolver(private val properties: EntitiesProperties) {
    init {
        detectDuplicates()
    }

    fun resolve(entityId: EntityId): EntityConfiguration? = properties
        .firstOrNull { it.selector.matches(entityId) }
        ?.toConfiguration(entityId)

    private fun EntityProperties.toConfiguration(entityId: EntityId) = EntityConfiguration(
        entityId,
        this.schema
    )

    private fun detectDuplicates() {
        val duplicatedSelectors = properties
            .flatMap { prop ->
                prop.selector
                    .toElementalSelectors()
                    .map { elemental -> elemental to prop }
            }
            .groupBy { it.first }
            .mapValues { it.value.size }
            .filter { it.value > 1 }
            .map { it.key }

        if (duplicatedSelectors.isNotEmpty())
            throw DuplicatedEntitySelectorsException(duplicatedSelectors)
    }
}
