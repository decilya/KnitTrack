package com.knittrac.app.presentation.feature_add_project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.service.AddProjectParams
import com.knittrac.app.domain.service.AddProjectService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

object AddProjectContract {
    data class State(
        val projectName: String = "",
        val selectedCategory: Category? = null,
        val categories: List<Category> = Category.values().toList(),
        val nameError: String? = null,
        val categoryError: String? = null
    )
    
    sealed class Intent {
        data class UpdateName(val name: String) : Intent()
        data class SelectCategory(val category: Category) : Intent()
        object SaveProject : Intent()
    }
    
    sealed class Effect {
        object ProjectAdded : Effect()
        data class ShowError(val message: String) : Effect()
    }
}

@HiltViewModel
class AddProjectViewModel @Inject constructor(
    private val addProjectService: AddProjectService
) : ViewModel() {
    
    private val _state = MutableStateFlow(AddProjectContract.State())
    val state: StateFlow<AddProjectContract.State> = _state.asStateFlow()
    
    private val _effect = Channel<AddProjectContract.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    fun onIntent(intent: AddProjectContract.Intent) {
        when (intent) {
            is AddProjectContract.Intent.UpdateName -> {
                _state.update { it.copy(projectName = intent.name, nameError = null) }
            }
            is AddProjectContract.Intent.SelectCategory -> {
                _state.update { it.copy(selectedCategory = intent.category, categoryError = null) }
            }
            is AddProjectContract.Intent.SaveProject -> {
                saveProject()
            }
        }
    }
    
    private fun saveProject() {
        val currentState = _state.value
        
        if (currentState.projectName.isBlank()) {
            _state.update { it.copy(nameError = "Название проекта не может быть пустым") }
            return
        }
        
        val category = currentState.selectedCategory
        if (category == null) {
            _state.update { it.copy(categoryError = "Выберите категорию") }
            return
        }
        
        viewModelScope.launch {
            val params = AddProjectParams(
                name = currentState.projectName,
                category = category
            )
            
            val result = addProjectService(params)
            if (result is Result.Success) {
                _effect.send(AddProjectContract.Effect.ProjectAdded)
            } else {
                _effect.send(AddProjectContract.Effect.ShowError("Ошибка сохранения проекта"))
            }
        }
    }
}
