package com.knittrack.app.domain.repository

import com.knittrack.app.core.common.Result
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.entity.PatternOperation
import com.knittrack.app.domain.entity.Project
import com.knittrack.app.domain.entity.Session
import com.knittrack.app.domain.entity.Symbol
import com.knittrack.app.domain.model.PatternRepeatWithId
import com.knittrack.app.domain.model.PatternRowWithId

/**
 * Контракт для работы с бэкапом на уровне БД.
 * 
 * Позволяет изолировать DataExporter и DataImporter от прямого использования Room.
 */
interface BackupRepository {
    suspend fun <T> withTransaction(block: suspend () -> T): T
    suspend fun savePatternsBatch(patterns: List<Pattern>)
    /**
     * Сохраняет батч рядов схем вместе с их patternId.
     *
     * @param rows Список пар (patternId, row).
     */
    suspend fun savePatternRowsBatch(rows: List<PatternRowWithId>)
    suspend fun saveSymbolsBatch(symbols: List<Symbol>)
    /**
     * Сохраняет батч раппортов схем вместе с их patternId.
     *
     * @param repeats Список пар (patternId, repeat).
     */
    suspend fun savePatternRepeatsBatch(repeats: List<PatternRepeatWithId>)
    /**
     * Сохраняет батч операций.
     * @return [Result.Success] или [Result.Error] при нарушении FK/триггеров.
     */
    suspend fun savePatternOperationsBatch(operations: List<PatternOperation>): Result<Unit>
    suspend fun saveProjectsBatch(batch: List<Project>)
    suspend fun saveSessionsBatch(batch: List<Session>)

    /**
     * Пересчитывает агрегированные totals проектов
     * (total_time_seconds, total_rows) из SUM(sessions) для всех проектов.
     *
     * Инвариант: projects.total_* == SUM(sessions.duration_seconds/row_count).
     * Импорт перезаписывает totals значениями из бэкапа, которые могут
     * расходиться с актуальной суммой сессий (бэкап старше последних
     * локальных сессий или totals устарели на момент экспорта). После
     * импорта DataImporterImpl вызывает этот метод для восстановления
     * инварианта.
     */
    suspend fun recomputeProjectTotals()

    suspend fun <T> exportSnapshot(block: suspend (ExportContext) -> T): T

    interface ExportContext {
        suspend fun forEachPattern(action: suspend (Pattern) -> Unit)
        /**
         * Итерирует по всем рядам всех схем (с patternId).
         *
         * @param action Колбэк, вызываемый для каждого ряда.
         */
        suspend fun forEachPatternRow(action: suspend (PatternRowWithId) -> Unit)
        suspend fun forEachSymbol(action: suspend (Symbol) -> Unit)
        /**
         * Итерирует по всем раппортам всех схем (с patternId).
         *
         * @param action Колбэк, вызываемый для каждого раппорта.
         */
        suspend fun forEachPatternRepeat(action: suspend (PatternRepeatWithId) -> Unit)
        suspend fun forEachPatternOperation(action: suspend (PatternOperation) -> Unit)
        suspend fun forEachProject(action: suspend (Project) -> Unit)
        suspend fun forEachSession(action: suspend (Session) -> Unit)
    }
}
