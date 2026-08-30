package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.AppErrorMapper
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.dao.ProjectDao
import com.knittrac.app.data.mapper.ProjectMapper.toDomain
import com.knittrac.app.data.mapper.ProjectMapper.toEntity
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация интерфейса [ProjectRepository] в слое данных (Data Layer).
 *
 * Отвечает за взаимодействие с локальной базой данных Room через [ProjectDao].
 *
 * **Важно:** Строго соблюдает **Правило 3** ТЗ:
 * При любой операции изменения (добавление, обновление, удаление, изменение времени)
 * автоматически проставляются актуальные значения полей:
 * - `updatedAt` = текущее системное время
 * - `syncStatus` = [SyncStatus.PENDING] (требует синхронизации с сервером)
 *
 * @param projectDao Интерфейс доступа к данным проектов (внедряется через Hilt).
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    /**
     * Возвращает реактивный поток всех проектов.
     *
     * Автоматически преобразует сущности базы данных ([ProjectEntity])
     * в доменные модели ([Project]) с помощью маппера при каждом изменении в БД.
     *
     * @return [Flow] со списком проектов, отсортированных по дате обновления (DESC).
     */
    override fun getAllProjectsFlow(): Flow<List<Project>> =
        projectDao.getAllProjects().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Получает конкретный проект по его уникальному идентификатору.
     *
     * @param id Уникальный идентификатор проекта.
     * @return [Result.Success] с доменной моделью проекта, или [Result.Error]
     *         если проект не найден или произошла ошибка базы данных.
     */
    override suspend fun getProjectById(id: Long): Result<Project> = try {
        val entity = projectDao.getProjectById(id)
        if (entity != null) {
            Result.Success(entity.toDomain())
        } else {
            Result.Error(AppError.ValidationError("Проект не найден"))
        }
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }

    /**
     * Добавляет новый проект в базу данных.
     *
     * Перед вставкой принудительно устанавливает `updatedAt` и `syncStatus = PENDING`
     * для соблюдения Правила 3 синхронизации.
     *
     * @param project Доменная модель проекта для добавления.
     * @return [Result.Success] с ID созданной записи, или [Result.Error] при сбое.
     */
    override suspend fun addProject(project: Project): Result<Long> = try {
        val entity = project.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        Result.Success(projectDao.insert(entity))
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }

    /**
     * Обновляет существующий проект в базе данных.
     *
     * Перед обновлением принудительно устанавливает `updatedAt` и `syncStatus = PENDING`
     * для соблюдения Правила 3 синхронизации.
     *
     * @param project Доменная модель проекта с обновленными данными.
     * @return [Result.Success] при успешном обновлении, или [Result.Error] при сбое.
     */
    override suspend fun updateProject(project: Project): Result<Unit> = try {
        val entity = project.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        projectDao.update(entity)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }

    /**
     * Удаляет проект по его идентификатору.
     *
     * Для строгого соблюдения Правила 3, перед физическим удалением из базы данных
     * мы формально обновляем метаданные сущности (updatedAt, syncStatus).
     *
     * @param id Уникальный идентификатор проекта для удаления.
     * @return [Result.Success] при успешном удалении, или [Result.Error] при сбое.
     */
    override suspend fun deleteProject(id: Long): Result<Unit> = try {
        projectDao.getProjectById(id)?.let { entity ->
            val entityToDelete = entity.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING.name
            )
            projectDao.delete(entityToDelete)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }

    /**
     * Инкрементально обновляет общее затраченное время проекта.
     *
     * Выполняет SQL-запрос UPDATE, который прибавляет переданное время к текущему
     * значению `totalTimeSeconds`. Также автоматически обновляет `updatedAt` и
     * `syncStatus` (Правило 3).
     *
     * @param projectId Уникальный идентификатор проекта.
     * @param additionalSeconds Время в секундах, которое нужно добавить к общему.
     *                          (Исправлено: имя параметра приведено в соответствие с интерфейсом).
     * @return [Result.Success] при успешном обновлении, или [Result.Error] при сбое.
     */
    override suspend fun updateTotalTime(projectId: Long, additionalSeconds: Long): Result<Unit> = try {
        projectDao.updateTotalTime(
            projectId = projectId,
            durationSeconds = additionalSeconds, // Передаем в DAO, где параметр называется durationSeconds
            updatedAt = System.currentTimeMillis()
        )
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppErrorMapper.map(e))
    }
}