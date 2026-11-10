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
    /**
     * Отмечает [row] как текущий ряд проекта.
     *
     * Порядок проверок:
     * 1. D-инвариант: currentStitch > 0 → currentPatternRowId != null
     *    (defense-in-depth к триггеру v8; writer сейчас ZERO, D недостижим
     *    через app-код — см. TECHDEBT P2-4).
     * 2. У проекта привязана схема.
     * 3. B-инвариант снят в v10 (3NF): currentRowNumber derived через JOIN.
     *
     * @param row Ряд схемы.
     * @return [Result.Success] с [MarkResult] (Added / AlreadyExists);
     *   [Result.Error] с [AppError.ValidationError] если нарушен D-инвариант,
     *   у проекта нет схемы.
     */
    fun markRow(row: PatternRow): Result<MarkResult> = when {
        currentStitch.value > 0 && currentPatternRowId == null ->
            Result.Error(AppError.ValidationError("Петли без ряда невозможны"))
        patternId == null ->
            Result.Error(AppError.ValidationError("У проекта нет привязанной схемы"))
        else -> successMarkResult(row)
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

