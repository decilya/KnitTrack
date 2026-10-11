package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [SessionRepository], инкапсулирующая логику работы с [SessionDao].
 * Отвечает за преобразование Entity в Domain-модели и обработку ошибок БД.
 *
 * Value Objects не покидают Domain слой — на границе с Room разворачиваются
 * в примитивы (Long). DAO не знает про VO.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    /**
     * Реактивный поток сессий проекта, отсортированных по времени начала
     * (сначала новые). Каждая Entity маппится в [Session].
     *
     * @param projectId Идентификатор проекта.
     * @return Flow списка Domain-сессий.
     */
    override fun getSessionsByProjectIdFlow(projectId: Long): Flow<List<Session>> =
        sessionDao.getSessionsByProjectIdFlow(projectId).map { list ->
            list.map { it.toDomain() }
        }

    /**
     * Разовый запрос сессий проекта.
     *
     * @param projectId Идентификатор проекта.
     * @return [Result.Success] со списком сессий или [Result.Error]
     *   с [AppError.DatabaseError] при сбое SQLite.
     */
    override suspend fun getSessionsByProjectId(projectId: Long): Result<List<Session>> = try {
        Result.Success(sessionDao.getSessionsByProjectId(projectId).map { it.toDomain() })
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Разовый запрос всех сессий.
     *
     * @return [Result.Success] со списком сессий или [Result.Error]
     *   с [AppError.DatabaseError] при сбое SQLite.
     */
    override suspend fun getAllSessions(): Result<List<Session>> = try {
        Result.Success(sessionDao.getAllSessions().map { it.toDomain() })
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Разовый запрос сессий со статусом PENDING (для будущей синхронизации).
     *
     * @return [Result.Success] со списком сессий или [Result.Error]
     *   с [AppError.DatabaseError] при сбое SQLite.
     */
    override suspend fun getPendingSessions(): Result<List<Session>> = try {
        Result.Success(sessionDao.getPendingSessions().map { it.toDomain() })
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Атомарно сохраняет сессию и увеличивает общее время проекта.
     *
     * Делегирует транзакцию в [SessionDao.insertSessionAndIncrementProjectTime]:
     * если что-то падает, обе операции откатываются. Длительность
     * разворачивается из Domain VO [com.knittrac.app.domain.entity.Duration]
     * в примитивный Long для DAO.
     *
     * @param session Domain-модель сессии.
     * @return [Result.Success] с id новой сессии или [Result.Error].
     */
    override suspend fun addSessionAtomically(session: Session): Result<Long> = try {
        val entity = session.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        val newRowId = sessionDao.insertSessionAndIncrementProjectTime(
            session = entity,
            durationSeconds = session.durationSeconds.seconds,
            updatedAt = entity.updatedAt
        )
        Result.Success(newRowId)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Обновляет существующую сессию (полная замена строки по id).
     * REPLACE работает как upsert.
     *
     * @param session Domain-модель сессии с актуальными полями.
     * @return [Result.Success] или [Result.Error].
     */
    override suspend fun updateSession(session: Session): Result<Unit> = try {
        val entity = session.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        sessionDao.insertSession(entity)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Обновляет статус синхронизации сессии.
     *
     * @param sessionId Идентификатор сессии.
     * @param status Новый статус (значение enum [SyncStatus]).
     * @return [Result.Success] или [Result.Error].
     */
    override suspend fun updateSyncStatus(sessionId: Long, status: String): Result<Unit> = try {
        sessionDao.updateSyncStatus(sessionId, status)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Агрегация статистики по дням для проекта.
     *
     * @param projectId Идентификатор проекта.
     * @return [Result.Success] со списком [DailyStat] или [Result.Error].
     */
    override suspend fun getDailyStats(projectId: Long): Result<List<DailyStat>> = try {
        Result.Success(sessionDao.getDailyStats(projectId))
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }
}
