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
 * Отвечает за преобразование Entity в Domain-модели и обработку ошибок базы данных.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    override fun getSessionsByProjectIdFlow(projectId: Long): Flow<List<Session>> =
        sessionDao.getSessionsByProjectIdFlow(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSessionsByProjectId(projectId: Long): Result<List<Session>> = try {
        Result.Success(sessionDao.getSessionsByProjectId(projectId).map { it.toDomain() })
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun getAllSessions(): Result<List<Session>> = try {
        Result.Success(sessionDao.getAllSessions().map { it.toDomain() })
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun getPendingSessions(): Result<List<Session>> = try {
        Result.Success(sessionDao.getPendingSessions().map { it.toDomain() })
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun addSessionAtomically(session: Session): Result<Long> = try {
        // Преобразуем Domain-модель в Entity и устанавливаем актуальные метаданные
        val entity = session.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        
        // Вызываем атомарный метод DAO, который гарантирует целостность данных
        val newRowId = sessionDao.insertSessionAndIncrementProjectTime(
            session = entity,
            durationSeconds = session.durationSeconds,
            updatedAt = entity.updatedAt
        )
        Result.Success(newRowId)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun updateSession(session: Session): Result<Unit> = try {
        val entity = session.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        sessionDao.insertSession(entity) // REPLACE сработает как upsert
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun updateSyncStatus(sessionId: Long, status: String): Result<Unit> = try {
        sessionDao.updateSyncStatus(sessionId, status)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun getDailyStats(projectId: Long): Result<List<DailyStat>> = try {
        Result.Success(sessionDao.getDailyStats(projectId))
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }
}
