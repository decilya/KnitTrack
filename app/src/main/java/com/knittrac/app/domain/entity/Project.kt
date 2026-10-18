package com.knittrac.app.domain.entity

import androidx.annotation.Keep
import com.knittrac.app.core.common.SyncStatus

/**
 * Доменная модель проекта вязания.
 *
 * Представляет бизнес-сущность проекта, независимую от деталей хранения в БД.
 * Название и общее время — Value Objects [ProjectName] и [Duration]:
 * защищают инварианты (непустое имя, неотрицательная длительность)
 * на уровне типа, а не в рантайме.
 *
 * Статус синхронизации — типобезопасный enum [SyncStatus], а не строка:
 * опечатка "PENDINGD" не скомпилируется.
 *
 * @property id Уникальный идентификатор проекта.
 * @property name Название проекта.
 * @property category Категория проекта (например, "Вязание спицами").
 * @property createdAt Временная метка создания проекта (в миллисекундах).
 * @property totalTimeSeconds Общее затраченное на проект время.
 * @property updatedAt Временная метка последнего обновления проекта.
 * @property syncStatus Статус синхронизации данных с облаком.
 */
@Keep
data class Project(
    val id: Long = 0L,
    val name: ProjectName,
    val category: Category,
    val createdAt: Long,
    val totalTimeSeconds: Duration = Duration.ZERO,
    val totalRows: Int = 0,
    val targetRowCount: Int? = null,
    val patternId: Long? = null,
    val currentRowNumber: Int = 0,
    val currentPatternRowId: Long? = null,
    val currentStitch: Int = 0,
    val updatedAt: Long,
    val syncStatus: SyncStatus
) {
    init {
        require(targetRowCount == null || targetRowCount >= 0) {
            "targetRowCount не может быть отрицательным"
        }
    }
}
