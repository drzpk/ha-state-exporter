package dev.drzepka.smarthome.haexporter.trait

import dev.drzepka.smarthome.haexporter.application.properties.ExporterProperties
import dev.drzepka.smarthome.haexporter.application.properties.SchemasProperties
import dev.drzepka.smarthome.haexporter.domain.properties.EntitiesProperties
import dev.drzepka.smarthome.haexporter.domain.value.StateMappings
import org.koin.core.module.Module
import org.koin.dsl.module

interface KoinTrait {
    val defaultConfigurationModule: Module
        get() = module {
            single { EntitiesProperties(emptyList()) }
            single { SchemasProperties(emptyList()) }
            single { StateMappings(emptyList()) }
            single { ExporterProperties() }
        }
}
