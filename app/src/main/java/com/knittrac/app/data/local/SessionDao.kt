package com.knittrac.app.data.local

import androidx.room.*
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.DailyStat
import kotlinx.coroutines.flow.Flow

private const val MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000L

/**
 * Интерфейс доступа к данным (DAO) для сущности Session.
 *
 * Содержит типобезопасные SQL-запросы для CRUD-операций и агрегации статистики.
 * Все имена колонок в @Query строго соответствуют @ColumnInfo в SessionEntity
 * (snake_case).
 *
 * UPDATE-методы возвращают Int (rows affected), чтобы репозиторий мог
 * различить "обновил 1 строку" и "id не найден".
 */
@Dao
interface SessionDao {

    /** Вставка сессии. REPLACE как upsert. @return id новой строки. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    /** Пакетная вставка (для импорта). @return список id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>): List<Long>

    /**
     * Обновление сессии по id.
     *
     * Отличается от [insertSession] с REPLACE: если строки с таким id нет —
     * вставки не будет, вернётся 0. Для create-пути использовать
     * [insertSessionAndIncrementProjectTime].
     *
     * @return количество затронутых строк: 1 — обновлено, 0 — id не найден.
     */
    @Update
    suspend fun updateSession(session: SessionEntity): Int

    /** Реактивный поток сессий проекта, сначала новые. */
    @Query("SELECT * FROM sessions WHERE project_id = :projectId ORDER BY start_time DESC")
    fun getSessionsByProjectIdFlow(projectId: Long): Flow<List<SessionEntity>>

    /** Разовый запрос сессий проекта, сначала новые. */
    @Query("SELECT * FROM sessions WHERE project_id = :projectId ORDER BY start_time DESC")
    suspend fun getSessionsByProjectId(projectId: Long): List<SessionEntity>

    /** Разовый запрос всех сессий. */
    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<SessionEntity>

    /** Разовый запрос сессий со статусом PENDING. */
    @Query("SELECT * FROM sessions WHERE sync_status = 'PENDING'")
    suspend fun getPendingSessions(): List<SessionEntity>

    /**
     * Обновление статуса синхронизации сессии.
     *
     * @return количество затронутых строк: 1 — обновлено, 0 — id не найден.
     */
    @Query("UPDATE sessions SET sync_status = :status WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: Long, status: String): Int

    /** Агрегация статистики по дням для проекта. */
    @Query("""
        SELECT
            (start_time / $MILLIS_IN_A_DAY) * $MILLIS_IN_A_DAY AS dayTimestamp,
            SUM(duration_seconds) AS totalSeconds
        FROM sessions
        WHERE project_id = :projectId
        GROUP BY dayTimestamp
        ORDER BY dayTimestamp DESC
    """)
    suspend fun getDailyStats(projectId: Long): List<DailyStat>

    /**
     * Внутренний метод для обновления общего времени проекта.
     * Используется исключительно внутри атомарной транзакции
     * [insertSessionAndIncrementProjectTime].
     *
     * @param pendingStatus Строковое имя статуса синхронизации (обычно
     *   SyncStatus.PENDING.name). Параметризовано, чтобы переименование
     *   enum-константы ловилось компилятором.
     * @return количество затронутых строк: 1 — обновлено, 0 — проекта нет.
     */
    @Query("""
        UPDATE projects
        SET total_time_seconds = total_time_seconds + :durationSeconds,
            updated_at = :updatedAt,
            sync_status = :pendingStatus
        WHERE id = :projectId
    """)
    suspend fun updateProjectTotalTime(
        projectId: Long,
        durationSeconds: Long,
        updatedAt: Long,
        pendingStatus: String = SyncStatus.PENDING.name
    ): Int

    /**
     * Атомарно добавляет сессию и обновляет время соответствующего проекта
     * в одной транзакции.
     *
     * @Transaction гарантирует: либо обе операции, либо ни одной. Если
     * падение на обновлении total_time — сессия тоже не сохранится.
     *
     * @param session Сущность сессии.
     * @param durationSeconds Длительность для инкремента времени проекта.
     * @param updatedAt Метка обновления.
     * @return id новой строки сессии.
     */
    @Transaction
    suspend fun insertSessionAndIncrementProjectTime(
        session: SessionEntity,
        durationSeconds: Long,
        updatedAt: Long
    ): Long {
        // 1. Сохраняем сессию (REPLACE как upsert).
        val newRowId = insertSession(session)
        // 2. Атомарно обновляем время проекта.
        // Возвращаемый Int не игнорируем: rows == 0 значит, что проекта
        // с таким id нет. check() бросает IllegalStateException, Room
        // откатывает @Transaction целиком — включая insertSession.
        val rows = updateProjectTotalTime(session.projectId, durationSeconds, updatedAt)
        check(rows == 1) { "Проект id=${session.projectId} не найден при обновлении total_time" }
        return newRowId
    }
}
