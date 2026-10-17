package com.knittrac.app.domain.repository

import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session

/**
 * Контракт для работы с бэкапом на уровне БД.
 * 
 * Позволяет изолировать DataExporter и DataImporter от прямого использования Room.
 */
interface BackupRepository {
    suspend fun <T> withTransaction(block: suspend () -> T): T
    suspend fun saveProjectsBatch(batch: List<Project>)
    suspend fun saveSessionsBatch(batch: List<Session>)
    suspend fun <T> exportSnapshot(block: suspend (ExportContext) -> T): T

    interface ExportContext {
        suspend fun forEachProject(action: suspend (Project) -> Unit)
        suspend fun forEachSession(action: suspend (Session) -> Unit)
    }
}
