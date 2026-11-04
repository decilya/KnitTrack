package com.knittrac.app.presentation.feature_timer

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.service.GetAllProjectsService
import com.knittrac.app.domain.service.MarkNextRowParams
import com.knittrac.app.domain.service.MarkNextRowUseCase
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
    private val markNextRowUseCase: MarkNextRowUseCase
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
}
