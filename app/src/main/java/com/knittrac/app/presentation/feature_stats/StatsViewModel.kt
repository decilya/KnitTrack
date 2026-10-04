package com.knittrac.app.presentation.feature_stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

object StatsContract {
    data class State(
        val dailyStats: List<DailyStat> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>("projectId") ?: 0L

    private val _state = MutableStateFlow(StatsContract.State())
    val state: StateFlow<StatsContract.State> = _state.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val result = sessionRepository.getDailyStats(projectId)
            if (result is Result.Success) {
                _state.update { it.copy(dailyStats = result.data, isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false, error = "Не удалось загрузить статистику") }
            }
        }
    }
}
