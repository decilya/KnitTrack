package com.knittrac.app.presentation.feature_projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.localization.CategoryLocalizer
import com.knittrac.app.domain.service.GetAllProjectsService
import com.knittrac.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MVI-контракт экрана списка проектов.
 */
object ProjectsContract {

    /**
     * UI-модель проекта для отображения в списке.
     *
     * Содержит только те данные, которые нужны экрану, включая
     * локализованное название категории. Domain-сущность [com.knittrac.app.domain.entity.Project]
     * не покидает Data/Domain слои.
     */
    data class ProjectUiModel(
        val id: Long,
        val name: String,
        val localizedCategoryName: String
    )

    data class State(
        val projects: List<ProjectUiModel> = emptyList(),
        val isLoading: Boolean = true,
        /** @StringRes id сообщения об ошибке. null — ошибки нет. */
        val errorRes: Int? = null
    )

    sealed class Effect {
        data class OpenTimer(val projectId: Long) : Effect()
    }
}

/**
 * ViewModel экрана списка проектов.
 *
 * Загружает проекты через [GetAllProjectsService], локализует категории через
 * [CategoryLocalizer] и обновляет State. После первого эмита (успех или ошибка)
 * выставляет `isLoading = false`.
 */
@HiltViewModel
class ProjectsListViewModel @Inject constructor(
    private val getAllProjectsService: GetAllProjectsService,
    private val categoryLocalizer: CategoryLocalizer
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsContract.State())
    val state: StateFlow<ProjectsContract.State> = _state.asStateFlow()

    private val _effect = Channel<ProjectsContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadProjects()
    }

    /**
     * Подписывается на поток проектов. Первый эмит (успех или ошибка) снимает
     * `isLoading`, чтобы UI показал список или EmptyState.
     */
    private fun loadProjects() {
        viewModelScope.launch {
            getAllProjectsService(Unit).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val uiProjects = result.data.map { project ->
                            ProjectsContract.ProjectUiModel(
                                id = project.id,
                                name = project.name,
                                localizedCategoryName = categoryLocalizer.localize(project.category)
                            )
                        }
                        _state.update { it.copy(projects = uiProjects, isLoading = false) }
                    }
                    is Result.Error -> {
                        Timber.e(result.error.cause, "Failed to load projects")
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorRes = R.string.error_load_projects
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Повторная попытка загрузки — вызывается из UI по кнопке Retry
     * после ошибки. Сбрасывает errorRes и перезапускает подписку.
     */
    fun retry() {
        _state.update { it.copy(isLoading = true, errorRes = null) }
        loadProjects()
    }
}
