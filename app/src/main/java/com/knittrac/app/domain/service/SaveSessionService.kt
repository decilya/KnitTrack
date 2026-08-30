package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.repository.ProjectRepository
import com.knittrac.app.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

/**
 * Сервис для сохранения сессии работы над проектом.
 *
 * Этот UseCase реализует **Правило 7** ТЗ:
 * 1. Вычисляет длительность сессии как `(end - start) / 1000`
 * 2. Если длительность <= 0 — возвращает ошибку валидации
 * 3. Сначала сохраняет сессию, затем обновляет общее время проекта
 * 4. Если обновление времени проекта упало — логирует ошибку,
 *    но возвращает успех (частичный успех)
 * 5. Если сохранение сессии упало — НЕ вызывает updateTotalTime,
 *    возвращает ошибку
 *
 * Такая логика обеспечивает целостность данных: сессия всегда сохраняется,
 * даже если агрегация времени не удалась.
 *
 * @param sessionRepository Репозиторий для работы с сессиями.
 * @param projectRepository Репозиторий для работы с проектами.
 * @param dispatcher Диспетчер для выполнения фоновой работы.
 *
 * @see SaveSessionParams Параметры для сохранения сессии.
 * @see ProjectRepository.updateTotalTime Метод для обновления общего времени.
 */
class SaveSessionService @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val projectRepository: ProjectRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<SaveSessionParams, Long>(dispatcher) {

    /**
     * Выполняет сохранение сессии согласно Правилу 7.
     *
     * Алгоритм:
     * 1. Вычисляет длительность в секундах
     * 2. Проверяет валидность (длительность > 0)
     * 3. Создает сессию с автоматическими полями
     * 4. Сохраняет сессию через репозиторий
     * 5. При успехе — обновляет общее время проекта
     * 6. При ошибке обновления времени — логирует, но возвращает успех
     *
     * @param params Параметры сессии (ID проекта, временные метки, количество рядов).
     * @return [Result.Success] с ID созданной сессии (даже если обновление времени не удалось)
     *         или [Result.Error] если сохранение сессии не удалось.
     */
    override suspend fun execute(params: SaveSessionParams): Result<Long> {
        // 1. Вычисляем длительность в секундах
        val durationSeconds = (params.endTimestamp - params.startTimestamp) / 1000L

        // 2. Проверяем валидность: сессия не может быть пустой или отрицательной
        if (durationSeconds <= 0L) {
            return Result.Error(
                AppError.ValidationError("Пустая сессия (длительность <= 0)")
            )
        }

        // 3. Создаем доменную модель сессии с автоматическими полями
        val session = Session(
            id = 0L,  // 0 означает новую сессию (Room сгенерирует ID)
            projectId = params.projectId,
            startTimestamp = params.startTimestamp,
            endTimestamp = params.endTimestamp,
            durationSeconds = durationSeconds,
            rowCount = params.rowCount,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING  // Требует синхронизации (Правило 3)
        )

        // 4. Сохраняем сессию через репозиторий и сразу обрабатываем результат
        return when (val addResult = sessionRepository.addSession(session)) {
            is Result.Success -> {
                // Сессия успешно сохранена — теперь обновляем общее время проекта
                val updateResult = projectRepository.updateTotalTime(
                    params.projectId,
                    durationSeconds
                )

                // 6. Проверяем результат обновления времени
                if (updateResult is Result.Error) {
                    // Логгируем ошибку, но НЕ прерываем операцию
                    // Это частичный успех: сессия сохранена, время не обновлено
                    Timber.e(
                        "Ошибка обновления общего времени проекта %s: %s",
                        params.projectId,
                        updateResult.error.message
                    )
                }

                // Возвращаем успех с ID сессии (даже если updateTotalTime упал)
                addResult
            }
            is Result.Error -> {
                // Сохранение сессии провалилось — НЕ вызываем updateTotalTime
                // Возвращаем ошибку
                addResult
            }
        }
    }
}