package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Сервис для удаления проекта по его ID.
 *
 * Этот UseCase делегирует операцию удаления репозиторию,
 * который автоматически проставляет updatedAt и syncStatus
 * перед удалением (Правило 3), хотя при физическом удалении это не требуется.
 *
 * @param projectRepository Репозиторий для работы с проектами.
 * @param dispatcher Диспетчер для выполнения фоновой работы.
 *
 * @see ProjectRepository.deleteProject Метод удаления проекта.
 */
class DeleteProjectService @Inject constructor(
    private val projectRepository: ProjectRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<Long, Unit>(dispatcher) {

    /**
     * Выполняет удаление проекта.
     *
     * Просто делегирует вызов репозиторию, который:
     * 1. Находит проект по ID
     * 2. Обновляет его поля (updatedAt, syncStatus)
     * 3. Удаляет из базы данных
     *
     * @param params Уникальный ID проекта для удаления.
     * @return [Result.Success] при успешном удалении или [Result.Error] при ошибке.
     */
    override suspend fun execute(params: Long): Result<Unit> =
        projectRepository.deleteProject(params)
}