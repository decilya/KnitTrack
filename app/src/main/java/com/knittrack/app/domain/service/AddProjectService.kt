package com.knittrack.app.domain.service

import com.knittrack.app.core.common.ErrorCode

import com.knittrack.app.core.base.BaseUseCase
import com.knittrack.app.core.common.AppError
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.common.SyncStatus
import com.knittrack.app.core.common.TimeProvider
import com.knittrack.app.core.di.IoDispatcher
import com.knittrack.app.domain.entity.Project
import com.knittrack.app.domain.entity.ProjectId
import com.knittrack.app.domain.entity.ProjectName
import com.knittrack.app.domain.entity.RowCount
import com.knittrack.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase создания нового проекта.
 *
 * Валидирует входное имя через Value Object [ProjectName]: пустая строка
 * или превышение длины превращаются в [Result.Error] с понятным сообщением.
 * Дополнительная защита поверх UI-валидации в AddProjectViewModel.
 */
class AddProjectService @Inject constructor(
    private val projectRepository: ProjectRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val timeProvider: TimeProvider
) : BaseUseCase<AddProjectParams, ProjectId>(dispatcher) {

    /**
     * Создаёт проект с валидным именем и PENDING-статусом синхронизации.
     *
     * @param params Параметры создания (сырое имя + категория).
     * @return [Result.Success] с [ProjectId] нового проекта; [Result.Error]
     *   с [AppError.ValidationError] если имя невалидно,
     *   или [AppError.DatabaseError] при сбое записи.
     */
    override suspend fun execute(params: AddProjectParams): Result<ProjectId> {
        val name = ProjectName.createOrNull(params.name)
            ?: return Result.Error(
                AppError.ValidationError(
                    ErrorCode.INVALID_ARGUMENT,
                    "name='${params.name}'"
                )
            )

        return buildProject(name, params).flatMap { project ->
            projectRepository.addProject(project)
        }
    }

    /**
     * Создаёт [Project] из валидированного имени и параметров.
     *
     * Возвращает [Result.Error] с [AppError.ValidationError], если инвариант
     * [Project] нарушен (targetRowCount < 0). Сообщение из IllegalArgumentException
     * сохраняется в ValidationError, чтобы не терять причину.
     *
     * Логирование здесь отсутствует: Domain не знает про Timber
     * (см. HANDOVER 4.5.1). Диагностика неожиданных IllegalArgumentException
     * выполняется в тестах через конкретный тип ошибки.
     */
    private fun buildProject(name: ProjectName, params: AddProjectParams): Result<Project> {
        return try {
            val now = timeProvider.currentTimeMillis()
            val targetRowCount = params.targetRowCount?.let { RowCount.create(it) }
            Result.Success(
                Project(
                    name = name,
                    category = params.category,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    targetRowCount = targetRowCount
                )
            )
        } catch (e: IllegalArgumentException) {
            Result.Error(AppError.ValidationError(ErrorCode.INVALID_ARGUMENT, e.message))
        }
    }
}
