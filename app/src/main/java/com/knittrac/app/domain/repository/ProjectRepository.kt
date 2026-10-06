package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Project
import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория для работы с проектами.
 * Инкапсулирует всю бизнес-логику и доступ к данным, возвращая только Domain-модели.
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
     * @param additionalSeconds Добавляемое время в секундах.
     * @return Result<Unit>
     */
    suspend fun updateTotalTime(projectId: Long, additionalSeconds: Long): Result<Unit>
}
