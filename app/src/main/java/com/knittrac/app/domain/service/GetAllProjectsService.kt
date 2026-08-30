package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseFlowUseCase
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Сервис для получения реактивного потока всех проектов.
 *
 * Этот UseCase предоставляет реактивный Flow, который автоматически
 * обновляется при любых изменениях в базе данных проектов.
 * Идеально подходит для отображения списка проектов в UI.
 *
 * @param projectRepository Репозиторий для работы с проектами.
 * @param dispatcher Диспетчер для выполнения фоновой работы.
 *
 * @see ProjectRepository.getAllProjectsFlow Метод получения потока.
 */
class GetAllProjectsService @Inject constructor(
    private val projectRepository: ProjectRepository,
    @param:IoDispatcher dispatcher: CoroutineDispatcher
) : BaseFlowUseCase<Unit, List<Project>>(dispatcher) {

    /**
     * Выполняет получение потока всех проектов.
     *
     * Параметры не требуются (Unit), так как получаем все проекты.
     *
     * @param params Пустой параметр (Unit).
     * @return [Flow] со списком всех проектов, отсортированных по дате обновления.
     */
    override fun execute(params: Unit): Flow<List<Project>> =
        projectRepository.getAllProjectsFlow()
}