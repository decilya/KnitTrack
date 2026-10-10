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
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("projectId")
    val projectId: Long,

    @SerializedName("startTimestamp")
    val startTimestamp: Long,

    @SerializedName("endTimestamp")
    val endTimestamp: Long,

    @SerializedName("durationSeconds")
    val durationSeconds: Long,

    @SerializedName("rowCount")
    val rowCount: Int,

    @SerializedName("updatedAt")
    val updatedAt: Long,

    @SerializedName("syncStatus")
    val syncStatus: String
)
