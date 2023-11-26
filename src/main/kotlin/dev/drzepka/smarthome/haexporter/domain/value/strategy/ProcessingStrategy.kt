package dev.drzepka.smarthome.haexporter.domain.value.strategy

interface ProcessingStrategy {
    fun getWorkUnits(): List<WorkUnit>
}
