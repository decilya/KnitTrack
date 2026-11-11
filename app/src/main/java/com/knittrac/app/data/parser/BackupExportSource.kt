package com.knittrac.app.data.parser

import com.knittrac.app.data.dto.PatternDto
import com.knittrac.app.data.dto.PatternOperationDto
import com.knittrac.app.data.dto.PatternRepeatDto
import com.knittrac.app.data.dto.PatternRowDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.data.dto.SymbolDto

/**
 * Источник данных для потоковой записи бэкапа.
 *
 * [BackupJsonParser] запрашивает данные по одному элементу, а [DataExporterImpl]
 * отдает их из [com.knittrac.app.domain.repository.BackupRepository.ExportContext].
 */
interface BackupExportSource {
    val version: String
    val exportDate: Long
    /** Итерирует по всем схемам. */
    suspend fun forEachPattern(action: suspend (PatternDto) -> Unit)
    /** Итерирует по всем рядам. */
    suspend fun forEachPatternRow(action: suspend (PatternRowDto) -> Unit)
    /** Итерирует по всем символам. */
    suspend fun forEachSymbol(action: suspend (SymbolDto) -> Unit)
    /** Итерирует по всем раппортам. */
    suspend fun forEachPatternRepeat(action: suspend (PatternRepeatDto) -> Unit)
    /** Итерирует по всем операциям. */
    suspend fun forEachPatternOperation(action: suspend (PatternOperationDto) -> Unit)
    /** Итерирует по всем проектам. */
    suspend fun forEachProject(action: suspend (ProjectDto) -> Unit)
    /** Итерирует по всем сессиям. */
    suspend fun forEachSession(action: suspend (SessionDto) -> Unit)
}
