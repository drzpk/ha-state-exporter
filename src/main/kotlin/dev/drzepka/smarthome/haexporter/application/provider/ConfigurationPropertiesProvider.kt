package dev.drzepka.smarthome.haexporter.application.provider

import dev.drzepka.smarthome.haexporter.application.properties.RootProperties

interface ConfigurationPropertiesProvider {
    val root: RootProperties
}
