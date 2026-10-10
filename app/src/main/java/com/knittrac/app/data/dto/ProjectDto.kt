package com.knittrac.app.data.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * DTO проекта для сериализации в JSON.
 *
 * Зачем отдельный DTO, а не Domain-сущность:
 * - Domain может меняться (переименование полей, Value Objects),
 *   формат бэкапа — нет.
 * - Явный @SerializedName фиксирует контракт: переименование
 *   Kotlin-поля не ломает старые бэкапы.
 * - @Keep защищает от R8 (удаление/переименование).
 *
 * Имена @SerializedName совпадают с историческим форматом,
 * записанным DataExporterImpl до введения DTO — старые бэкапы
 * читаются без миграции.
 */
@Keep
data class ProjectDto(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("createdAt")
    val createdAt: Long,

    @SerializedName("totalTimeSeconds")
    val totalTimeSeconds: Long = 0L,

    @SerializedName("updatedAt")
    val updatedAt: Long,

    @SerializedName("syncStatus")
    val syncStatus: String
)
