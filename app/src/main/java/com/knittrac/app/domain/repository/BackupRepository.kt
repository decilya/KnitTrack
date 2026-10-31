package com.knittrac.app.domain.repository

import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRepeat
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.entity.Symbol

/**
 * Контракт для работы с бэкапом на уровне БД.
 * 
 * Позволяет изолировать DataExporter и DataImporter от прямого использования Room.
 */
interface BackupRepository {
    suspend fun <T> withTransaction(block: suspend () -> T): T
    suspend fun savePatternsBatch(patterns: List<Pattern>)
    suspend fun savePatternRowsBatch(rows: List<Pair<Long, PatternRow>>)
    suspend fun saveSymbolsBatch(symbols: List<Symbol>)
    suspend fun savePatternRepeatsBatch(repeats: List<Pair<Long, PatternRepeat>>)
    suspend fun savePatternOperationsBatch(operations: List<PatternOperation>)
    suspend fun saveProjectsBatch(batch: List<Project>)
    suspend fun saveSessionsBatch(batch: List<Session>)
    suspend fun <T> exportSnapshot(block: suspend (ExportContext) -> T): T

    interface ExportContext {
        suspend fun forEachPattern(action: suspend (Pattern) -> Unit)
        suspend fun forEachPatternRow(action: suspend (patternId: Long, row: PatternRow) -> Unit)
        suspend fun forEachSymbol(action: suspend (Symbol) -> Unit)
        suspend fun forEachPatternRepeat(action: suspend (patternId: Long, repeat: PatternRepeat) -> Unit)
        suspend fun forEachPatternOperation(action: suspend (PatternOperation) -> Unit)
        suspend fun forEachProject(action: suspend (Project) -> Unit)
        suspend fun forEachSession(action: suspend (Session) -> Unit)
    }
}
