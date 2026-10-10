package com.knittrac.app.data.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Корневой DTO бэкапа.
 *
 * Формат JSON:
 * {
 *   "version": "1.0",
 *   "exportDate": 1234567890,
 *   "projects": [ ... ],
 *   "sessions": [ ... ]
 * }
 *
 * Поле version — String, а не Int. Это позволяет точечные
 * версии ("1.1", "2.0-beta") без ломающей смены типа. При
 * импорте неизвестная версия логируется через Timber.w, но
 * читается как текущий формат — forward-compatible.
 */
@Keep
data class ExportDataDto(
    @SerializedName("version")
    val version: String,

    @SerializedName("exportDate")
    val exportDate: Long,

    @SerializedName("projects")
    val projects: List<ProjectDto> = emptyList(),

    @SerializedName("sessions")
    val sessions: List<SessionDto> = emptyList()
) {
    companion object {
        /**
         * Текущая версия формата. Увеличивается при несовместимых
         * изменениях структуры DTO. Добавление опциональных полей
         * версию не меняет — старые бэкапы остаются совместимыми.
         */
        const val CURRENT_VERSION = "1.0"
    }
}
