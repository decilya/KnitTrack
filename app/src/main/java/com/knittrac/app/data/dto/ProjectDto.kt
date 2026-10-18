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
 */
@Keep
data class ProjectDto(
    /** Уникальный идентификатор проекта. */
    @SerializedName("id")
    val id: Long = 0L,

    /** Название проекта. */
    @SerializedName("name")
    val name: String,

    /** Категория проекта. */
    @SerializedName("category")
    val category: String,

    /** Временная метка создания (Unix epoch). */
    @SerializedName("createdAt")
    val createdAt: Long,

    /** Общее время вязания (секунды). */
    @SerializedName("totalTimeSeconds")
    val totalTimeSeconds: Long = 0L,

    /** Всего связано рядов (накопительно). */
    @SerializedName("totalRows")
    val totalRows: Int = 0,

    /** Целевое количество рядов (для прогресс-бара). */
    @SerializedName("targetRowCount")
    val targetRowCount: Int? = null,

    /**
     * Идентификатор привязанной схемы.
     * Null, если проект ведется без схемы.
     */
    @SerializedName("patternId")
    val patternId: Long? = null,

    /**
     * Текущий ряд по схеме (для UI). Может отличаться от общего счетчика.
     */
    @SerializedName("currentRowNumber")
    val currentRowNumber: Int = 0,

    /**
     * Идентификатор текущего ряда в схеме. Null, если схемы нет.
     */
    @SerializedName("currentPatternRowId")
    val currentPatternRowId: Long? = null,

    /**
     * Текущая петля в ряду. Используется для крючка и бисера.
     */
    @SerializedName("currentStitch")
    val currentStitch: Int = 0,

    /** Временная метка последнего обновления (Unix epoch). */
    @SerializedName("updatedAt")
    val updatedAt: Long,

    /** Статус синхронизации. */
    @SerializedName("syncStatus")
    val syncStatus: String
)
