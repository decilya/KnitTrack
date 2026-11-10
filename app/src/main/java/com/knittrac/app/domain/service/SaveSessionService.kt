package com.knittrac.app.domain.service

import com.knittrac.app.core.common.ErrorCode

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.TimeProvider
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.entity.SessionId
import com.knittrac.app.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase для сохранения сессии вязания.
 *
 * Реализует Правило 7 ТЗ: делегирует атомарное сохранение в [SessionRepository],
 * что гарантирует целостность данных на уровне БД. Сессия и время проекта
 * обновляются в одной транзакции, исключая возможность "частичного успеха".
 *
 * Длительность вычисляется из меток времени и упаковывается в Value Object
 * [Duration], гарантирующий неотрицательность на уровне типа.
 */
class SaveSessionService @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val timeProvider: TimeProvider,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<SaveSessionParams, SessionId>(dispatcher) {
    /**
     * Сохраняет сессию и атомарно обновляет время проекта.
     *
     * Вызывается через [BaseUseCase.invoke]; выполняется на [dispatcher].
     *
     * @param params Параметры сессии для сохранения (включая временные метки
     *   и количество рядов).
     * @return [Result.Success] с ID новой сессии или [Result.Error] при сбое
     *   валидации или БД.
     */
    override suspend fun execute(params: SaveSessionParams): Result<SessionId> {
        // 1. Валидация: длительность должна быть строго больше нуля секунд.
        val durationSeconds = (params.endTimestamp - params.startTimestamp) / 1000L
        if (durationSeconds <= 0L) {
            return Result.Error(AppError.ValidationError(ErrorCode.EMPTY_SESSION))
        }
        val duration = Duration.create(durationSeconds)

        // 2. Сборка Domain-модели из параметров.
        val session = Session(
            projectId = params.projectId,
            startTimestamp = params.startTimestamp,
            endTimestamp = params.endTimestamp,
            durationSeconds = duration,
            rowCount = params.rowCount,
            updatedAt = timeProvider.currentTimeMillis()
        )

        // 3. Атомарное сохранение. При сбое транзакция откатится целиком.
        return sessionRepository.addSessionAtomically(session)
    }
}
