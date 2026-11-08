package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.StitchCount
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase для обновления счетчика петель в текущем ряду.
 *
 * Защищает D-инвариант (`stitch > 0 -> row != null`):
 * если у проекта нет текущего ряда, возвращает ValidationError.
 */
class UpdateStitchUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<UpdateStitchParams, StitchCount>(dispatcher) {

    override suspend fun execute(params: UpdateStitchParams): Result<StitchCount> {
        return when (val projectResult = projectRepository.getProjectById(params.projectId)) {
            is Result.Error -> projectResult
            is Result.Success -> {
                val project = projectResult.data
                val currentRowId = project.currentPatternRowId
                if (currentRowId == null) {
                    Result.Error(AppError.ValidationError("Нельзя считать петли без выбранного ряда"))
                } else {
                    val result = projectRepository.updateProjectProgress(
                        projectId = params.projectId,
                        currentPatternRowId = currentRowId,
                        currentStitch = params.newStitch
                    )
                    when (result) {
                        is Result.Error -> result
                        is Result.Success -> Result.Success(params.newStitch)
                    }
                }
            }
        }
    }
}
