package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Сервис для получения агрегированной дневной статистики по проекту.
 *
 * Этот UseCase возвращает статистику работы над проектом, сгруппированную
 * по дням. Используется для построения графиков активности.
 *
 * @param sessionRepository Репозиторий для работы с сессиями.
 * @param dispatcher Диспетчер для выполнения фоновой работы.
 *
 * @see SessionRepository.getDailyStats Метод получения статистики.
 * @see DailyStat Сущность дневной статистики.
 */
class GetProjectStatisticsService @Inject constructor(
    private val sessionRepository: SessionRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<Long, List<DailyStat>>(dispatcher) {

    /**
     * Выполняет получение дневной статистики.
     *
     * @param params Уникальный ID проекта.
     * @return [Result.Success] со списком [DailyStat] (отсортированных по дате)
     *         или [Result.Error] при ошибке.
     */
    override suspend fun execute(params: Long): Result<List<DailyStat>> =
        sessionRepository.getDailyStats(params)
}