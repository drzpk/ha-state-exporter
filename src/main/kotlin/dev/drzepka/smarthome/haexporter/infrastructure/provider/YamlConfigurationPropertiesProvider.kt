package dev.drzepka.smarthome.haexporter.infrastructure.provider

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.drzepka.smarthome.haexporter.application.properties.RootProperties
import dev.drzepka.smarthome.haexporter.application.provider.ConfigurationPropertiesProvider
import java.io.File
import java.lang.IllegalArgumentException

class YamlConfigurationPropertiesProvider private constructor(source: String) : ConfigurationPropertiesProvider {
    override val root: RootProperties

    init {
        val mapper = YAMLMapper()
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        mapper.propertyNamingStrategy = PropertyNamingStrategies.SnakeCaseStrategy()
        mapper.registerKotlinModule()

        root = mapper.readValue(source)
    }

    companion object {
        private const val CONFIG_LOCATION_ENV = "CONFIG_LOCATION"

        fun fromEnvironmentFile(): YamlConfigurationPropertiesProvider {
            val fileLocation = System.getProperty(CONFIG_LOCATION_ENV)
                ?: System.getenv(CONFIG_LOCATION_ENV)
                ?: throw IllegalArgumentException("The environment variable '$CONFIG_LOCATION_ENV' is not set")

            try {
                val text = if (fileLocation.startsWith("classpath:"))
                    loadTextFromResource(fileLocation.substringAfter("classpath:"))
                else
                    File(fileLocation).readText()

                return fromString(text)
            } catch (e: Exception) {
                throw IllegalArgumentException("Error while loading configuration from file: $fileLocation", e)
            }
        }

        fun fromString(text: String) = YamlConfigurationPropertiesProvider(text)

        private fun loadTextFromResource(location: String): String {
            val stream = YamlConfigurationPropertiesProvider::class.java.classLoader.getResourceAsStream(location)
                ?: throw IllegalStateException("Resource doesn't exist: $location")

            return stream.readAllBytes().decodeToString()
        }
    }
}
