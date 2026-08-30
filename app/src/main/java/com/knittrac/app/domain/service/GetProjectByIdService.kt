package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Сервис для получения проекта по его уникальному ID.
 *
 * Этот UseCase предоставляет доступ к данным конкретного проекта.
 * Если проект не найден, возвращает [Result.Error] с сообщением "Не найден".
 *
 * @param projectRepository Репозиторий для работы с проектами.
 * @param dispatcher Диспетчер для выполнения фоновой работы.
 *
 * @see ProjectRepository.getProjectById Метод получения проекта.
 */
class GetProjectByIdService @Inject constructor(
    private val projectRepository: ProjectRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<Long, Project>(dispatcher) {

    /**
     * Выполняет получение проекта по ID.
     *
     * @param params Уникальный ID проекта.
     * @return [Result.Success] с проектом, если он найден,
     *         или [Result.Error] если проект не существует.
     */
    override suspend fun execute(params: Long): Result<Project> =
        projectRepository.getProjectById(params)
}