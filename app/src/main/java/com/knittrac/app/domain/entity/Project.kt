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
    val id: ProjectId = ProjectId.ZERO,
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
        return validateDenormalization(row) ?: successMarkResult(row)
    }

    /**
     * Валидирует согласованность денормализации: если `currentPatternRowId`
     * указывает на `row`, то `currentRowNumber` должен соответствовать
     * `row.rowNumber`. null — валидно, не null — [Result.Error].
     */
    private fun validateDenormalization(row: PatternRow): Result.Error? = when {
        currentPatternRowId != row.id -> null
        currentRowNumber == RowCount.createOrDefault(row.rowNumber.value) -> null
        else -> {
            val expected = RowCount.createOrDefault(row.rowNumber.value)
            Result.Error(
                AppError.ValidationError(
                    "Рассинхронизация прогресса: currentRowNumber=${currentRowNumber.value}, " +
                        "rowNumber=${expected.value} для ряда id=${row.id.value}"
                )
            )
        }
    }

    private fun successMarkResult(row: PatternRow): Result.Success<MarkResult> =
        Result.Success(
            if (currentPatternRowId == row.id) MarkResult.AlreadyExists
            else MarkResult.Added
        )
}

/**
 * Может ли пользователь открыть Pattern Editor для этого проекта.
 *
 * Возвращает `true`, только если к проекту привязана схема (`patternId != null`).
 * Единственная точка проверки для двух входов в редактор:
 * - `TimerScreen` — иконка Edit в `TopAppBar`;
 * - `ProjectsListScreen` — LongPress на карточке → «Открыть схему».
 *
 * При расширении условий — править здесь (SRP/OCP, G2).
 */
fun Project.canOpenPatternEditor(): Boolean = patternId != null

