package dev.drzepka.smarthome.haexporter

import dev.drzepka.smarthome.haexporter.application.configuration.haStateExporterModule
import dev.drzepka.smarthome.haexporter.application.properties.ExporterProperties
import dev.drzepka.smarthome.haexporter.application.service.JobScheduler
import dev.drzepka.smarthome.haexporter.application.service.StateExporter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.logging.log4j.kotlin.Logging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

fun main() {
    val handler = CoroutineExceptionHandler { _, exception ->
        HaStateExporter.logger.error("Uncaught Coroutines exception", exception)
    }

    val scope = CoroutineScope(Dispatchers.Default + Job() + CoroutineName("HSE-scope") + handler)
    startKoin {
        val haStateExporter = HaStateExporter(scope)
        modules(haStateExporterModule(scope))
        haStateExporter.start()
    }
}

private class HaStateExporter(private val scope: CoroutineScope) : KoinComponent {
    private val scheduler: JobScheduler by inject()
    private val exporter: StateExporter by inject()
    private val properties: ExporterProperties by inject()

    fun start() {
        logger.info("Starting HA state exporter")

        var initialLaunch = true
        scheduler.scheduleJob("exporter", properties.interval) {
            if (!initialLaunch)
                exporter.export()
        }

        scope.launch {
            exporter.export()
            initialLaunch = false
        }

        while (scope.isActive) {
            Thread.sleep(500)
        }

        logger.info("Stopping HA state exporter")
    }

    companion object : Logging
}
