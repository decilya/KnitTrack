package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.repository.SessionRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase для сохранения сессии вязания.
 * 
 * Реализует Правило 7 ТЗ: делегирует атомарное сохранение в [SessionRepository], 
 * что гарантирует целостность данных на уровне БД. Сессия и время проекта обновляются 
 * в одной транзакции, исключая возможность "частичного успеха".
 */
class SaveSessionService @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    /**
     * Сохраняет сессию и атомарно обновляет время проекта.
     * 
     * @param params Параметры сессии для сохранения (включая временные метки и количество рядов).
     * @return [Result.Success] с ID новой сессии или [Result.Error] при сбое валидации или БД.
     */
    suspend operator fun invoke(params: SaveSessionParams): Result<Long> {
        // 1. Валидация: длительность сессии должна быть строго больше нуля
        val durationSeconds = (params.endTimestamp - params.startTimestamp) / 1000L
        if (durationSeconds <= 0L) {
            return Result.Error(AppError.ValidationError("Пустая сессия"))
        }

        // 2. Сборка Domain-модели из параметров
        val session = Session(
            projectId = params.projectId,
            startTimestamp = params.startTimestamp,
            endTimestamp = params.endTimestamp,
            durationSeconds = durationSeconds,
            rowCount = params.rowCount
        )

        // 3. Атомарное сохранение. Если что-то пойдет не так, транзакция откатится целиком.
        return when (val result = sessionRepository.addSessionAtomically(session)) {
            is Result.Success -> result
            is Result.Error -> {
                Timber.e(result.error.cause, "Не удалось атомарно сохранить сессию")
                result
            }
        }
    }
}
