package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.model.MarkResult
import com.knittrac.app.domain.repository.PatternRepository
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase отметки ряда как связанного.
 *
 * Application-слой, тонкая обёртка над [ProgressService.markRow]:
 * загружает агрегаты [Project] и [Pattern], находит [PatternRow] по id
 * и делегирует cross-aggregate проверку в [ProgressService].
 *
 * ВАЖНО: [ProgressService.markRow] тоже загружает [Pattern] для проверки
 * принадлежности ряда — в текущей реализации [Pattern] загружается дважды
 * (P2 в TECHDEBT: оптимизация через `ProgressService.markRow(projectId, rowId)`).
 *
 * @see ProgressService
 * @see MarkRowParams
 */
class MarkRowUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val patternRepository: PatternRepository,
    private val progressService: ProgressService,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<MarkRowParams, MarkResult>(dispatcher) {

    /**
     * Выполняет отметку ряда.
     *
     * @param params Идентификаторы проекта и ряда.
     * @return [Result.Success] с [MarkResult] (Added / AlreadyExists);
     *   [Result.Error] с [AppError.ValidationError] если у проекта нет схемы
     *   или ряд не найден; [AppError.NotFoundError] / [AppError.DatabaseError]
     *   при сбое репозитория.
     */
    override suspend fun execute(params: MarkRowParams): Result<MarkResult> {
        return when (val projectResult = projectRepository.getProjectById(params.projectId)) {
            is Result.Error -> projectResult
            is Result.Success -> continueAfterProject(projectResult.data, params)
        }
    }

    /**
     * Продолжает после загрузки проекта: извлекает patternId и загружает [Pattern].
     */
    private suspend fun continueAfterProject(
        project: Project,
        params: MarkRowParams
    ): Result<MarkResult> {
        val patternId: PatternId = project.patternId
            ?: return Result.Error(AppError.ValidationError("У проекта нет привязанной схемы"))

        return when (val patternResult = patternRepository.getPatternById(patternId)) {
            is Result.Error -> patternResult
            is Result.Success -> findRowAndMark(project, patternResult.data, params)
        }
    }

    /**
     * Находит [PatternRow] по id и делегирует в [ProgressService.markRow].
     */
    private suspend fun findRowAndMark(
        project: Project,
        pattern: Pattern,
        params: MarkRowParams
    ): Result<MarkResult> {
        val row = pattern.rows.firstOrNull { it.id == params.rowId }
            ?: return Result.Error(
                AppError.ValidationError("Ряд id=${params.rowId.value} не найден в схеме")
            )
        return progressService.markRow(project, row)
    }
}
