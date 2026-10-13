package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория для работы с проектами.
 * Инкапсулирует всю бизнес-логику и доступ к данным, возвращая только
 * Domain-модели.
 *
 * Обновление общего времени принимает Value Object [Duration] —
 * неотрицательность гарантируется типом на уровне компилятора.
 */
interface ProjectRepository {
    /** Получает реактивный поток всех проектов. */
    fun getAllProjectsFlow(): Flow<List<Project>>

    /** Получает статический список всех проектов. */
    suspend fun getAllProjects(): Result<List<Project>>

    /**
     * Получает конкретный проект по его ID.
     *
     * @return [Result.Success] с проектом или [Result.Error]
     *   с [com.knittrac.app.core.common.AppError.NotFoundError], если проект не найден.
     */
    suspend fun getProjectById(id: Long): Result<Project>

    /** Добавляет новый проект в хранилище. */
    suspend fun addProject(project: Project): Result<Long>

    /** Обновляет существующий проект. */
    suspend fun updateProject(project: Project): Result<Unit>

    /** Удаляет проект по его ID. */
    suspend fun deleteProject(id: Long): Result<Unit>

    /**
     * Обновляет статус синхронизации проекта.
     *
     * Симметричен [SessionRepository.updateSyncStatus]: позволяет
     * перевести проект в SYNCED после успешного аплоада. Без этого
     * проекта навсегда остаётся в PENDING (updateProject и updateTotalTime
     * принудительно проставляют PENDING).
     *
     * @param projectId Идентификатор проекта.
     * @param status Новый статус синхронизации.
     * @return [Result.Success], [Result.Error] с
     *   [com.knittrac.app.core.common.AppError.NotFoundError] если проекта нет,
     *   или [com.knittrac.app.core.common.AppError.DatabaseError].
     */
    suspend fun updateSyncStatus(projectId: Long, status: SyncStatus): Result<Unit>

    /**
     * Атомарно обновляет общее затраченное время проекта.
     *
     * @param projectId Идентификатор проекта.
     * @param duration Добавляемое время (Value Object [Duration]).
     * @return [Result.Success] или [Result.Error].
     */
    suspend fun updateTotalTime(projectId: Long, duration: Duration): Result<Unit>
}
