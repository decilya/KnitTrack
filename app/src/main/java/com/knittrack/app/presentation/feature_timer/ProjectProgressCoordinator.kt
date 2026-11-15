package com.knittrack.app.presentation.feature_timer

import com.knittrack.app.core.common.Result
import com.knittrack.app.domain.entity.Project
import com.knittrack.app.domain.entity.ProjectId
import com.knittrack.app.domain.entity.RowNumber
import com.knittrack.app.domain.service.GetAllProjectsService
import com.knittrack.app.domain.service.UpdateStitchUseCase
import com.knittrack.app.domain.service.UpdateStitchParams
import com.knittrack.app.domain.entity.StitchCount
import com.knittrack.app.domain.service.MarkNextRowParams
import com.knittrack.app.domain.service.MarkNextRowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Координатор прогресса проекта по схеме (P1-C, 02.11.2026).
 *
 * SRP: одна причина изменения — «прогресс по схеме» (загрузка списка проектов
 * + отметка следующего ряда).
 * Выделен из [TimerViewModel] (5 причин изменения → 2 координатора + VM-оркестратор).
 *
 * Ответственность:
 * - Прокси к [GetAllProjectsService] (Flow списка проектов).
 * - Отметка следующего ряда через [MarkNextRowUseCase].
 *
 * НЕ содержит State — его собирает [TimerViewModel].
 *
 * @property getAllProjectsService Поток всех проектов (Domain use case).
 * @property markNextRowUseCase Отметка следующего ряда (Domain use case).
 */
class ProjectProgressCoordinator @Inject constructor(
    private val getAllProjectsService: GetAllProjectsService,
    private val markNextRowUseCase: MarkNextRowUseCase,
    private val updateStitchUseCase: UpdateStitchUseCase
) {

    /** Реактивный поток списка проектов с [Result] (Success / Error). */
    fun projects(): Flow<Result<List<Project>>> = getAllProjectsService(Unit)

    /**
     * Отмечает следующий ряд схемы выбранного проекта.
     *
     * @param projectId Идентификатор проекта.
     * @return [Result.Success] с номером ряда; [Result.Error] с ValidationError
     *   (нет схемы / последний ряд / пустая схема) или DatabaseError.
     */
    suspend fun markNext(projectId: ProjectId): Result<RowNumber> =
        markNextRowUseCase(MarkNextRowParams(projectId = projectId))

    /**
     * Обновляет счетчик петель для текущего ряда.
     */
    suspend fun updateStitch(projectId: ProjectId, newStitch: StitchCount): Result<StitchCount> =
        updateStitchUseCase(UpdateStitchParams(projectId, newStitch))
}
