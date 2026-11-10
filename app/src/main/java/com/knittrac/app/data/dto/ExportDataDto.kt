package com.knittrac.app.data.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Корневой DTO бэкапа.
 *
 * Формат JSON:
 * {
 *   "version": "1.2",
 *   "exportDate": 1234567890,
 *   "patterns": [ ... ],
 *   "patternRows": [ ... ],
 *   "symbols": [ ... ],
 *   "patternRepeats": [ ... ],
 *   "patternOperations": [ ... ],
 *   "projects": [ ... ],
 *   "sessions": [ ... ]
 * }
 *
 * 1.1 объявлял patterns/patternRows, но writer их не писал.
 * 1.2 пишет пять таблиц схем. Импорт 1.0/1.1 обнуляет FK проекта.
 *
 * Поле version — String, а не Int. Это позволяет точечные
 * версии ("1.1", "2.0-beta") без ломающей смены типа. При
 * импорте неизвестная версия логируется через Timber.w, но
 * читается как текущий формат — forward-compatible.
 */
@Keep
data class ExportDataDto(
    /** Версия формата бэкапа. */
    @SerializedName("version")
    val version: String,

    /** Временная метка экспорта (Unix epoch). */
    @SerializedName("exportDate")
    val exportDate: Long,

    /** Список схем. Пишется до projects: у проекта FK на схему. */
    @SerializedName("patterns")
    val patterns: List<PatternDto> = emptyList(),

    /** Список рядов схем. */
    @SerializedName("patternRows")
    val patternRows: List<PatternRowDto> = emptyList(),

    /** Символы (системные и кастомные). */
    @SerializedName("symbols")
    val symbols: List<SymbolDto> = emptyList(),

    /** Раппорты схем. */
    @SerializedName("patternRepeats")
    val patternRepeats: List<PatternRepeatDto> = emptyList(),

    /** Операции рядов. */
    @SerializedName("patternOperations")
    val patternOperations: List<PatternOperationDto> = emptyList(),

    /** Список проектов. */
    @SerializedName("projects")
    val projects: List<ProjectDto> = emptyList(),

    /** Список сессий. */
    @SerializedName("sessions")
    val sessions: List<SessionDto> = emptyList()
) {
    companion object {
        /**
         * Текущая версия формата. 1.2 добавляет таблицы схем.
         * Файлы 1.0 и 1.1 читаются, но без привязки проекта к схеме.
         */
        const val CURRENT_VERSION = "1.3"
    }
}
