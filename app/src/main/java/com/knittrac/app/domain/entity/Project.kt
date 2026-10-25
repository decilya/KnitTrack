package com.knittrac.app.domain.entity

import androidx.annotation.Keep
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.model.MarkResult

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
    val totalRows: RowCount = RowCount.ZERO,
    val targetRowCount: RowCount? = null,
    val patternId: PatternId? = null,
    val currentRowNumber: RowCount = RowCount.ZERO,
    val currentPatternRowId: PatternRowId? = null,
    val currentStitch: StitchCount = StitchCount.ZERO,
    val updatedAt: Long,
    val syncStatus: SyncStatus
) {
    fun markRow(row: PatternRow): Result<MarkResult> {
        if (patternId == null) {
            return Result.Error(AppError.ValidationError("У проекта нет привязанной схемы"))
        }
        return Result.Success(
            if (currentPatternRowId == row.id) MarkResult.AlreadyExists
            else MarkResult.Added
        )
    }
}
