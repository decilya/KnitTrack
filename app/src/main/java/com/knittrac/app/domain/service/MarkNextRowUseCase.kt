package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase отметки следующего ряда схемы как связанного.
 *
 * Application-слой, тонкая обёртка над [ProgressService.markNextRow]:
 * загружает [Project], делегирует выбор «следующего» ряда и его сохранение.
 * Логика «next = rowNumber + 1» — в Domain Service, не здесь (DDD).
 *
 * @see ProgressService.markNextRow
 * @see MarkNextRowParams
 */
class MarkNextRowUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val progressService: ProgressService,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<MarkNextRowParams, RowNumber>(dispatcher) {

    /**
     * Отмечает следующий ряд схемы.
     *
     * @param params Идентификатор проекта.
     * @return [Result.Success] с номером отмеченного ряда;
     *   [Result.Error] с [com.knittrac.app.core.common.AppError.ValidationError]
     *   при нарушении инварианта (нет схемы / последний ряд / пустые rows)
     *   или [com.knittrac.app.core.common.AppError.NotFoundError] / DatabaseError.
     */
    override suspend fun execute(params: MarkNextRowParams): Result<RowNumber> {
        return when (val projectResult = projectRepository.getProjectById(params.projectId.value)) {
            is Result.Error -> projectResult
            is Result.Success -> progressService.markNextRow(projectResult.data)
        }
    }
}
