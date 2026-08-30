package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Project
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с проектами.
 *
 * Этот интерфейс определяет контракт для доступа к данным проектов.
 * Реализация находится в data слое (com.knittrac.app.data.repository.ProjectRepositoryImpl)
 * и отвечает за взаимодействие с базой данных Room.
 *
 * Основные принципы:
 * - Возвращает [Result] для явной обработки успеха/ошибки
 * - Предоставляет реактивный Flow для наблюдения за изменениями
 * - Автоматически проставляет updatedAt и syncStatus
 *   при любых операциях изменения (Правило 3)
 *
 * @see com.knittrac.app.data.repository.ProjectRepositoryImpl Реализация репозитория в data слое.
 * @see Project Доменная сущность проекта.
 */
interface ProjectRepository {
    /**
     * Возвращает реактивный поток всех проектов.
     *
     * Этот Flow автоматически обновляется при любых изменениях в базе данных,
     * что позволяет UI реактивно обновляться без дополнительных вызовов.
     *
     * @return [Flow] со списком всех проектов, отсортированных по updatedAt
     *         (последние обновленные — первыми).
     */
    fun getAllProjectsFlow(): Flow<List<Project>>

    /**
     * Получает проект по его уникальному идентификатору.
     *
     * @param id Уникальный идентификатор проекта.
     * @return [Result] с проектом, если он найден, или [Result.Error] с ошибкой,
     *         если проект не существует.
     */
    suspend fun getProjectById(id: Long): Result<Project>

    /**
     * Добавляет новый проект в базу данных.
     *
     * Автоматически проставляет:
     * - updatedAt = текущее время
     * - syncStatus = PENDING (Правило 3)
     *
     * @param project Проект для добавления.
     * @return [Result] с уникальным идентификатором созданного проекта
     *         или [Result.Error] при ошибке.
     */
    suspend fun addProject(project: Project): Result<Long>

    /**
     * Обновляет существующий проект.
     *
     * Автоматически проставляет:
     * - updatedAt = текущее время
     * - syncStatus = PENDING (Правило 3)
     *
     * @param project Проект с обновленными данными.
     * @return [Result.Success] при успешном обновлении или [Result.Error] при ошибке.
     */
    suspend fun updateProject(project: Project): Result<Unit>

    /**
     * Удаляет проект по его идентификатору.
     *
     * Автоматически проставляет updatedAt и syncStatus
     * перед удалением (Правило 3), хотя при физическом удалении это не требуется.
     *
     * @param id Уникальный идентификатор проекта для удаления.
     * @return [Result.Success] при успешном удалении или [Result.Error] при ошибке.
     */
    suspend fun deleteProject(id: Long): Result<Unit>

    /**
     * Обновляет общее время проекта, добавляя указанную длительность.
     *
     * Этот метод используется сервисом com.knittrac.app.domain.service.SaveSessionService
     * для агрегации времени из всех сессий проекта. Выполняет SQL UPDATE с инкрементом:
     * `totalTimeSeconds = totalTimeSeconds + additionalSeconds`
     *
     * Автоматически проставляет:
     * - updatedAt = текущее время
     * - syncStatus = PENDING (Правило 3)
     *
     * @param projectId Уникальный идентификатор проекта.
     * @param additionalSeconds Дополнительное время в секундах для добавления.
     * @return [Result.Success] при успешном обновлении или [Result.Error] при ошибке.
     *
     * @see com.knittrac.app.domain.service.SaveSessionService Сервис, который вызывает этот метод.
     */
    suspend fun updateTotalTime(projectId: Long, additionalSeconds: Long): Result<Unit>
}