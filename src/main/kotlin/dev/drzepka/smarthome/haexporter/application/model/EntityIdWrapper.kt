package dev.drzepka.smarthome.haexporter.application.model

import dev.drzepka.smarthome.haexporter.domain.value.EntityId

data class EntityIdWrapper<T>(val entityId: EntityId, val value: T)
