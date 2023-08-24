package dev.drzepka.smarthome.haexporter.infrastructure.provider

import dev.drzepka.smarthome.haexporter.application.properties.RootProperties
import dev.drzepka.smarthome.haexporter.application.provider.ConfigurationPropertiesProvider

class DirectConfigurationProvider(override val root: RootProperties) : ConfigurationPropertiesProvider
