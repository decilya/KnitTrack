package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
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

    /** Получает конкретный проект по его ID. */
    suspend fun getProjectById(id: Long): Result<Project>

    /** Добавляет новый проект в хранилище. */
    suspend fun addProject(project: Project): Result<Long>

    /** Обновляет существующий проект. */
    suspend fun updateProject(project: Project): Result<Unit>

    /** Удаляет проект по его ID. */
    suspend fun deleteProject(id: Long): Result<Unit>

    /**
     * Атомарно обновляет общее затраченное время проекта.
     *
     * @param projectId Идентификатор проекта.
     * @param duration Добавляемое время (Value Object [Duration]).
     * @return [Result.Success] или [Result.Error].
     */
    suspend fun updateTotalTime(projectId: Long, duration: Duration): Result<Unit>
}
