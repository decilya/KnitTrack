package com.knittrac.app.presentation.feature_projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.service.GetAllProjectsService
import dagger.hilt.android.lifecycle.HiltViewModel
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
 *
 * `isLoading` критичен для правильного UX: без него при загрузке
 * мигнёт EmptyStateView до появления списка.
 */
object ProjectsContract {

    data class State(
        val projects: List<Project> = emptyList(),
        /** true — данные ещё загружаются (показываем CircularProgressIndicator). */
        val isLoading: Boolean = true
    )

    sealed class Effect {
        data class OpenTimer(val projectId: Long) : Effect()
    }
}

/**
 * ViewModel экрана списка проектов.
 *
 * Загружает проекты через [GetAllProjectsService] и обновляет State.
 * После первого эмита (успех или ошибка) выставляет `isLoading = false`,
 * чтобы Screen переключился с индикатора загрузки на список или EmptyState.
 */
@HiltViewModel
class ProjectsListViewModel @Inject constructor(
    private val getAllProjectsService: GetAllProjectsService
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsContract.State())
    val state: StateFlow<ProjectsContract.State> = _state.asStateFlow()

    private val _effect = Channel<ProjectsContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadProjects()
    }

    /**
     * Подписывается на поток проектов.
     * Первый эмит снимает `isLoading`, чтобы UI показал список или EmptyState.
     */
    private fun loadProjects() {
        viewModelScope.launch {
            getAllProjectsService(Unit).collect { result ->
                when (result) {
                    is Result.Success -> _state.update {
                        it.copy(projects = result.data, isLoading = false)
                    }
                    is Result.Error -> _state.update {
                        // Даже при ошибке — снимаем isLoading, чтобы UI не висел
                        it.copy(isLoading = false)
                    }
                }
            }
        }
    }
}
