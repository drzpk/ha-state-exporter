package dev.drzepka.smarthome.haexporter.application.configuration

import dev.drzepka.smarthome.haexporter.application.properties.SchemasProperties
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.application.provider.HomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.application.service.JobScheduler
import dev.drzepka.smarthome.haexporter.application.service.StateExporter
import dev.drzepka.smarthome.haexporter.application.service.StatePipeline
import dev.drzepka.smarthome.haexporter.domain.properties.EntitiesProperties
import dev.drzepka.smarthome.haexporter.domain.repository.ExportStatusRepository
import dev.drzepka.smarthome.haexporter.domain.repository.StateRepository
import dev.drzepka.smarthome.haexporter.domain.service.EntityConfigurationResolver
import dev.drzepka.smarthome.haexporter.domain.service.EntityIdResolver
import dev.drzepka.smarthome.haexporter.domain.service.ProcessingStrategyResolver
import dev.drzepka.smarthome.haexporter.domain.service.StateMapper
import dev.drzepka.smarthome.haexporter.domain.service.StateValueConverter
import dev.drzepka.smarthome.haexporter.domain.value.StateMappings
import dev.drzepka.smarthome.haexporter.infrastructure.database.InfluxDBClientProvider
import dev.drzepka.smarthome.haexporter.infrastructure.database.SQLConnectionProvider
import dev.drzepka.smarthome.haexporter.infrastructure.provider.APIHomeAssistantEntityMetadataProvider
import dev.drzepka.smarthome.haexporter.infrastructure.provider.SQLHomeAssistantStateProvider
import dev.drzepka.smarthome.haexporter.infrastructure.provider.YamlConfigurationPropertiesProvider
import dev.drzepka.smarthome.haexporter.infrastructure.repository.InfluxDBExportStatusRepository
import dev.drzepka.smarthome.haexporter.infrastructure.repository.InfluxDBStateRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module
import java.time.Clock

val domainModule = module {
    single { EntityConfigurationResolver(get()) }
    single { EntityIdResolver(get()) }
    single { ProcessingStrategyResolver(get(), get()) }
    single { StateValueConverter() }
    single { StateMapper(get(), get()) }
}

val mariaDBModule = module {
    single { SQLConnectionProvider(get()) }
    single<HomeAssistantStateProvider> { SQLHomeAssistantStateProvider(get()) }
}

val influxDBModule = module {
    single { InfluxDBClientProvider(get()) }
    single<StateRepository> { InfluxDBStateRepository(get()) }
    single<ExportStatusRepository> { InfluxDBExportStatusRepository(get()) }
}

val infrastructureModule = module {
    val root by lazy { YamlConfigurationPropertiesProvider.fromEnvironmentFile().root }
    single { root.homeAssistant.database }
    single { root.homeAssistant.api }
    single { root.influxDB }
    single { root.exporter }
    single { root.exporter.processing }
    single { EntitiesProperties(root.entities) }
    single { SchemasProperties(root.schemas) }
    single { StateMappings(root.stateMappings) }
    single<HomeAssistantEntityMetadataProvider> { APIHomeAssistantEntityMetadataProvider(root.homeAssistant.api) }
}

val applicationModule = module {
    single { StatePipeline(get(), get(), get(), get(), get()) }
    single { StateExporter(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { JobScheduler(get(), get()) }
}

fun haStateExporterModule(scope: CoroutineScope) = module(createdAtStart = true) {
    single { Clock.systemDefaultZone() }
    single { scope }
    includes(infrastructureModule, mariaDBModule, influxDBModule, domainModule, applicationModule)
}
