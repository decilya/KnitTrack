package com.knittrac.app.data.repository
import com.knittrac.app.core.common.AppErrorMapper
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.dao.SessionDao
import com.knittrac.app.data.mapper.SessionMapper.toDomain
import com.knittrac.app.data.mapper.SessionMapper.toEntity
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class SessionRepositoryImpl @Inject constructor(private val sessionDao: SessionDao) : SessionRepository {
    override suspend fun addSession(session: Session): Result<Long> = try { Result.Success(sessionDao.insert(session.toEntity().copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING.name))) } catch (e: Exception) { Result.Error(AppErrorMapper.map(e)) }
    override suspend fun getSessionsByProjectId(projectId: Long): Result<List<Session>> = try { Result.Success(sessionDao.getSessionsByProjectId(projectId).map { it.toDomain() }) } catch (e: Exception) { Result.Error(AppErrorMapper.map(e)) }
    override suspend fun getDailyStats(projectId: Long): Result<List<com.knittrac.app.domain.entity.DailyStat>> = try { Result.Success(sessionDao.getDailyStats(projectId)) } catch (e: Exception) { Result.Error(AppErrorMapper.map(e)) }
    override suspend fun getAllSessions(): Result<List<Session>> = try { Result.Success(sessionDao.getAllSessions().map { it.toDomain() }) } catch (e: Exception) { Result.Error(AppErrorMapper.map(e)) }
}
