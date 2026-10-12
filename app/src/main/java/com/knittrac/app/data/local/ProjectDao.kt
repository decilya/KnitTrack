package com.knittrac.app.data.local

import androidx.room.*
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с таблицей projects.
 *
 * Все UPDATE-методы возвращают Int (rows affected), чтобы репозиторий мог
 * различить "обновил 1 строку" и "обновил 0 строк = записи нет".
 */
@Dao
interface ProjectDao {

    /** Реактивный поток всех проектов, сначала новые. */
    @Query("SELECT * FROM projects ORDER BY created_at DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    /** Разовый запрос всех проектов, сначала новые. */
    @Query("SELECT * FROM projects ORDER BY created_at DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    /** Запрос проекта по id. null — если не найден. */
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    /** Вставка проекта. REPLACE как upsert. @return id новой строки. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    /** Пакетная вставка (для импорта). @return список id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>): List<Long>

    /**
     * Обновление проекта по id.
     *
     * @return количество затронутых строк: 1 — обновлено, 0 — id не найден.
     */
    @Update
    suspend fun updateProject(project: ProjectEntity): Int

    /** Удаление проекта. Каскад на sessions через FK CASCADE. */
    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    /**
     * Удаляет проект по id без предварительного чтения.
     * FK CASCADE на sessions выполняется на уровне БД.
     *
     * @return количество затронутых строк: 1 — удалено, 0 — записи не было.
     *   rows=0 — не ошибка: удаление идемпотентно.
     */
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long): Int

    /**
     * Атомарный инкремент total_time_seconds + updated_at + PENDING.
     *
     * @param pendingStatus Строковое имя статуса синхронизации (обычно
     *   SyncStatus.PENDING.name). Параметризовано, чтобы переименование
     *   enum-константы ловилось компилятором, а не тихо уезжало в данные.
     * @return количество затронутых строк: 1 — обновлено, 0 — id не найден.
     */
    @Query("""
        UPDATE projects
        SET total_time_seconds = total_time_seconds + :durationSeconds,
            updated_at = :updatedAt,
            sync_status = :pendingStatus
        WHERE id = :projectId
    """)
    suspend fun updateTotalTime(
        projectId: Long,
        durationSeconds: Long,
        updatedAt: Long,
        pendingStatus: String = SyncStatus.PENDING.name
    ): Int
}
