package dev.drzepka.smarthome.haexporter.application.configuration

import dev.drzepka.smarthome.haexporter.application.provider.ConfigurationPropertiesProvider
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.application.service.StateExporter
import dev.drzepka.smarthome.haexporter.application.service.StatePipeline
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.service.EntityConfigurationResolver
import dev.drzepka.smarthome.haexporter.domain.service.EntityIdResolver
import dev.drzepka.smarthome.haexporter.domain.service.StateMapper
import dev.drzepka.smarthome.haexporter.domain.service.StateValueConverter
import dev.drzepka.smarthome.haexporter.infrastructure.database.InfluxDBClientProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrastructure.provider.SQLHomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.infrastructure.repository.InfluxDBStateRepository
import org.koin.dsl.module

val domainModule = module {
    single { EntityConfigurationResolver(get<ConfigurationPropertiesProvider>().root.entities) }
    single { EntityIdResolver(get<ConfigurationPropertiesProvider>().root.entities) }
    single {
        val mappings = get<ConfigurationPropertiesProvider>().root.stateMappings
        StateMapper().apply { mappings.forEach { registerMapping(it) } }
    }
    single { StateValueConverter() }

}

val mariaDBModule = module {
    single { get<ConfigurationPropertiesProvider>().root.homeAssistant }
    single { SQLConnectionProvider(get()) }
    single<HomeAssistantStateProvider> { SQLHomeAssistantStateProvider(get()) }
}

val influxDBModule = module {
    single { get<ConfigurationPropertiesProvider>().root.influxDB }
    single { InfluxDBClientProvider(get()) }
    single<StateRepository> { InfluxDBStateRepository(get()) }
}

val infrastructureModule = module {

}

val applicationModule = module {
    single { get<ConfigurationPropertiesProvider>().root.exporter }
    single { StatePipeline(get(), get(), get(), get(), get<ConfigurationPropertiesProvider>().root.schemas, get()) }
    single { StateExporter(get(), get(), get(), get(), get(), get()) }
}

val haStateExporterModule = module {
    includes(infrastructureModule, influxDBModule, domainModule, applicationModule)
}
