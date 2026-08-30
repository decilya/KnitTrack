package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.entity.Session

/**
 * Интерфейс репозитория для работы с сессиями.
 *
 * Этот интерфейс определяет контракт для доступа к данным сессий работы
 * над проектами. Реализация находится в data слое
 * (com.knittrac.app.data.repository.SessionRepositoryImpl).
 *
 * Основные принципы:
 * - Возвращает [Result] для явной обработки успеха/ошибки
 * - Автоматически проставляет updatedAt и syncStatus
 *   при операциях изменения (Правило 3)
 * - Предоставляет агрегированную статистику через [getDailyStats]
 *
 * @see com.knittrac.app.data.repository.SessionRepositoryImpl Реализация репозитория в data слое.
 * @see Session Доменная сущность сессии.
 */
interface SessionRepository {
    /**
     * Добавляет новую сессию в базу данных.
     *
     * Автоматически проставляет:
     * - updatedAt = текущее время
     * - syncStatus = PENDING (Правило 3)
     *
     * @param session Сессия для добавления.
     * @return [Result] с уникальным идентификатором созданной сессии
     *         или [Result.Error] при ошибке.
     */
    suspend fun addSession(session: Session): Result<Long>

    /**
     * Получает все сессии для указанного проекта.
     *
     * Сессии возвращаются отсортированными по времени начала
     * (последние — первыми).
     *
     * @param projectId Идентификатор проекта.
     * @return [Result] со списком всех сессий проекта или [Result.Error] при ошибке.
     */
    suspend fun getSessionsByProjectId(projectId: Long): Result<List<Session>>

    /**
     * Получает агрегированную дневную статистику для проекта.
     *
     * Этот метод возвращает список [DailyStat], где каждый элемент содержит:
     * - [DailyStat.dayTimestamp] — начало дня (00:00:00)
     * - [DailyStat.totalSeconds] — общее время работы в этот день
     *
     * Агрегация выполняется через SQL GROUP BY с округлением времени
     * до начала суток: `(startTimestamp / 86400000) * 86400000`
     *
     * @param projectId Идентификатор проекта.
     * @return [Result] со списком дневной статистики или [Result.Error] при ошибке.
     *
     * @see DailyStat Сущность дневной статистики.
     */
    suspend fun getDailyStats(projectId: Long): Result<List<DailyStat>>

    /**
     * Получает все сессии из базы данных.
     *
     * Используется для экспорта данных (com.knittrac.app.domain.service.ExportDataService).
     *
     * @return [Result] со списком всех сессий или [Result.Error] при ошибке.
     */
    suspend fun getAllSessions(): Result<List<Session>>
}