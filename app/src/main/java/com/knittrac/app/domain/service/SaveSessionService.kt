package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.repository.ProjectRepository
import com.knittrac.app.domain.repository.SessionRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase для сохранения сессии вязания.
 * 
 * Реализует Правило 7 ТЗ: сохранить сессию, затем атомарно обновить 
 * общее время проекта. При ошибке обновления времени — логирует её, 
 * но возвращает успех (частичный успех, так как сессия уже сохранена).
 */
class SaveSessionService @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val projectRepository: ProjectRepository
) {
    /**
     * Сохраняет сессию и обновляет время проекта.
     * 
     * @param params Параметры сессии для сохранения.
     * @return [Result.Success] с ID сессии или [Result.Error] при сбое.
     */
    suspend operator fun invoke(params: SaveSessionParams): Result<Long> {
        val durationSeconds = (params.endTimestamp - params.startTimestamp) / 1000L
        if (durationSeconds <= 0L) {
            return Result.Error(AppError.ValidationError("Пустая сессия"))
        }

        val session = Session(
            projectId = params.projectId,
            startTimestamp = params.startTimestamp,
            endTimestamp = params.endTimestamp,
            durationSeconds = durationSeconds,
            rowCount = params.rowCount
        )

        val addResult = sessionRepository.addSession(session)
        return when (addResult) {
            is Result.Success -> {
                val updateResult = projectRepository.updateTotalTime(
                    projectId = params.projectId,
                    additionalSeconds = durationSeconds
                )
                if (updateResult is Result.Error) {
                    Timber.e("Не удалось обновить время проекта ${params.projectId}")
                }
                addResult
            }
            is Result.Error -> addResult
        }
    }
}
