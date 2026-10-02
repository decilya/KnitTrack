package com.knittrac.app.presentation.feature_projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
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

object ProjectsContract {
    data class State(
        val projects: List<com.knittrac.app.domain.entity.Project> = emptyList()
    )
    
    sealed class Effect {
        data class OpenTimer(val projectId: Long) : Effect()
    }
}

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
    
    private fun loadProjects() {
        viewModelScope.launch {
            getAllProjectsService(Unit).collect { result ->
                if (result is Result.Success) {
                    _state.update { it.copy(projects = result.data) }
                }
            }
        }
    }
}
