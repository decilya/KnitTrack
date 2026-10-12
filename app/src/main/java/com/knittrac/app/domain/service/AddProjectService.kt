package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.repository.ProjectRepository
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
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<AddProjectParams, Long>(dispatcher) {

    /**
     * Создаёт проект с валидным именем и PENDING-статусом синхронизации.
     *
     * @param params Параметры создания (сырое имя + категория).
     * @return [Result.Success] с id нового проекта, [Result.Error]
     *   с [AppError.ValidationError] если имя невалидно,
     *   или [AppError.DatabaseError] при сбое записи.
     */
    override suspend fun execute(params: AddProjectParams): Result<Long> {
        val name = ProjectName.createOrNull(params.name)
            ?: return Result.Error(
                AppError.ValidationError(
                    "Название проекта не может быть пустым или длиннее " +
                    "${ProjectName.MAX_LENGTH} символов"
                )
            )

        val now = System.currentTimeMillis()
        val project = Project(
            name = name,
            category = params.category,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING
        )
        return projectRepository.addProject(project)
    }
}
