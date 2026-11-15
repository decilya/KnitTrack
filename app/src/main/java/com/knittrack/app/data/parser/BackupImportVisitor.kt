package com.knittrack.app.data.parser

import com.knittrack.app.data.dto.PatternDto
import com.knittrack.app.data.dto.PatternOperationDto
import com.knittrack.app.data.dto.PatternRepeatDto
import com.knittrack.app.data.dto.PatternRowDto
import com.knittrack.app.data.dto.ProjectDto
import com.knittrack.app.data.dto.SessionDto
import com.knittrack.app.data.dto.SymbolDto

/**
 * Колбэки для потокового чтения бэкапа.
 *
 * Позволяет [BackupJsonParser] не загружать весь JSON в память: данные
 * передаются батчами по мере парсинга.
 */
interface BackupImportVisitor {
    /** Вызывается при чтении поля `version`. */
    suspend fun onVersion(version: String)
    /** Вызывается при чтении батча схем. */
    suspend fun onPatternsBatch(batch: List<PatternDto>)
    /** Вызывается при чтении батча рядов. */
    suspend fun onPatternRowsBatch(batch: List<PatternRowDto>)
    /** Вызывается при чтении батча символов. */
    suspend fun onSymbolsBatch(batch: List<SymbolDto>)
    /** Вызывается при чтении батча раппортов. */
    suspend fun onPatternRepeatsBatch(batch: List<PatternRepeatDto>)
    /** Вызывается при чтении батча операций. */
    suspend fun onPatternOperationsBatch(batch: List<PatternOperationDto>)
    /** Вызывается при чтении батча проектов. */
    suspend fun onProjectsBatch(batch: List<ProjectDto>)
    /** Вызывается при чтении батча сессий. */
    suspend fun onSessionsBatch(batch: List<SessionDto>)
}
