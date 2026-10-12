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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [SessionRepository], инкапсулирующая логику работы с [SessionDao].
 * Отвечает за преобразование Entity в Domain-модели и обработку ошибок БД.
 *
 * UPDATE-методы проверяют rows affected: 0 строк = сессия не найдена,
 * возвращаем [AppError.NotFoundError] вместо молчаливого Success.
 *
 * Все catch-блоки сначала отлавливают [CancellationException] и пробрасывают
 * её дальше: отмена корутины — не ошибка, а сигнал. Без этого
 * viewModelScope.cancel() превращался бы в DatabaseError и ломал бы
 * cleanup в finally-блоках.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    /**
     * Реактивный поток сессий проекта, сначала новые.
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
     * @return [Result.Success] со списком или [Result.Error].
     */
    override suspend fun getSessionsByProjectId(projectId: Long): Result<List<Session>> = try {
        Result.Success(sessionDao.getSessionsByProjectId(projectId).map { it.toDomain() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Разовый запрос всех сессий.
     *
     * @return [Result.Success] со списком или [Result.Error].
     */
    override suspend fun getAllSessions(): Result<List<Session>> = try {
        Result.Success(sessionDao.getAllSessions().map { it.toDomain() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Разовый запрос сессий со статусом PENDING.
     *
     * @return [Result.Success] со списком или [Result.Error].
     */
    override suspend fun getPendingSessions(): Result<List<Session>> = try {
        Result.Success(sessionDao.getPendingSessions().map { it.toDomain() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Атомарно сохраняет сессию и инкрементит total_time проекта.
     *
     * Делегирует в [SessionDao.insertSessionAndIncrementProjectTime]: при
     * падении на второй операции сессия тоже откатывается.
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
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Обновляет существующую сессию (полная замена по id).
     *
     * Не создаёт новую строку. Для create-пути использовать
     * [SessionRepository.addSessionAtomically].
     *
     * ВАЖНО: ранее был upsert через REPLACE — при отсутствии строки
     * с таким id создавалась новая. Сейчас чистый UPDATE.
     * Семантика изменилась — вызывающий код должен учитывать.
     *
     * ВАЖНО: поля updatedAt и syncStatus входной модели игнорируются —
     * репозиторий проставляет их сам.
     *
     * @param session Domain-модель с актуальными полями.
     * @return [Result.Success], [Result.Error] с [AppError.NotFoundError]
     *   или [AppError.DatabaseError].
     */
    override suspend fun updateSession(session: Session): Result<Unit> = try {
        val entity = session.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        val rows = sessionDao.updateSession(entity)
        if (rows == 0) {
            Result.Error(AppError.NotFoundError("Сессия id=${session.id} не найдена"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Обновляет статус синхронизации сессии.
     *
     * @param sessionId Идентификатор сессии.
     * @param status Новый статус.
     * @return [Result.Success], [Result.Error] с [AppError.NotFoundError]
     *   или [AppError.DatabaseError].
     */
    override suspend fun updateSyncStatus(sessionId: Long, status: SyncStatus): Result<Unit> = try {
        val rows = sessionDao.updateSyncStatus(sessionId, status.name)
        if (rows == 0) {
            Result.Error(AppError.NotFoundError("Сессия id=$sessionId не найдена"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Агрегация статистики по дням для проекта.
     *
     * @return [Result.Success] со списком [DailyStat] или [Result.Error].
     */
    override suspend fun getDailyStats(projectId: Long): Result<List<DailyStat>> = try {
        Result.Success(sessionDao.getDailyStats(projectId))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e))
    }
}
