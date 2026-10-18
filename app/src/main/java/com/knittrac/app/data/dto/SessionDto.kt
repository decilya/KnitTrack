package com.knittrac.app.data.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * DTO сессии для сериализации в JSON.
 *
 * Имена @SerializedName совпадают с историческим форматом —
 * старые бэкапы читаются без миграции.
 */
@Keep
data class SessionDto(
    /** Уникальный идентификатор сессии. */
    @SerializedName("id")
    val id: Long = 0L,

    /** Идентификатор проекта, к которому относится сессия. */
    @SerializedName("projectId")
    val projectId: Long,

    /** Временная метка начала сессии (Unix epoch). */
    @SerializedName("startTimestamp")
    val startTimestamp: Long,

    /** Временная метка окончания сессии (Unix epoch). */
    @SerializedName("endTimestamp")
    val endTimestamp: Long,

    /** Длительность сессии в секундах. */
    @SerializedName("durationSeconds")
    val durationSeconds: Long,

    /** Количество рядов, связанных за сессию. */
    @SerializedName("rowCount")
    val rowCount: Int,

    /** Временная метка последнего обновления (Unix epoch). */
    @SerializedName("updatedAt")
    val updatedAt: Long,

    /** Статус синхронизации. */
    @SerializedName("syncStatus")
    val syncStatus: String
)
