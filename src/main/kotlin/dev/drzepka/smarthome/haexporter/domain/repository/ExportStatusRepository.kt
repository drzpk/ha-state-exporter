package dev.drzepka.smarthome.haexporter.domain.repository

import dev.drzepka.smarthome.haexporter.domain.entity.ExportStatus

interface ExportStatusRepository {
    suspend fun save(status: ExportStatus)
}
