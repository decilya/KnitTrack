package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Сервис для добавления нового проекта.
 *
 * Этот UseCase отвечает за создание нового проекта в приложении.
 * Он инициализирует все необходимые поля (createdAt, updatedAt, syncStatus)
 * и передает проект в репозиторий для сохранения.
 *
 * @param projectRepository Репозиторий для работы с проектами (внедряется через Hilt).
 * @param dispatcher Диспетчер для выполнения фоновой работы (помечен [IoDispatcher]).
 *
 * @see ProjectRepository Интерфейс репозитория проектов.
 * @see AddProjectParams Параметры для добавления проекта.
 */
class AddProjectService @Inject constructor(
    private val projectRepository: ProjectRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<AddProjectParams, Long>(dispatcher) {

    /**
     * Выполняет добавление нового проекта.
     *
     * Создает экземпляр Project с автоматически установленными полями:
     * - createdAt = текущее время
     * - updatedAt = текущее время
     * - syncStatus = PENDING (для синхронизации)
     *
     * Затем передает проект в репозиторий для сохранения.
     *
     * @param params Параметры добавления (название и категория).
     * @return [Result.Success] с уникальным ID созданного проекта
     *         или [Result.Error] при ошибке сохранения.
     */
    override suspend fun execute(params: AddProjectParams): Result<Long> {
        // Получаем текущее время для установки временных меток
        val now = System.currentTimeMillis()

        // Создаем новый проект с автоматически установленными полями
        val project = Project(
            name = params.name,
            category = params.category,
            createdAt = now,           // Время создания
            updatedAt = now,           // Время последнего обновления
            syncStatus = SyncStatus.PENDING  // Требует синхронизации (Правило 3)
        )

        // Сохраняем проект через репозиторий и возвращаем результат
        return projectRepository.addProject(project)
    }
}