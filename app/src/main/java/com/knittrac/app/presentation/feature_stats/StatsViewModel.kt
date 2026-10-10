package com.knittrac.app.presentation.feature_stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knittrac.app.R
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
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
        /** @StringRes id сообщения об ошибке. null — ошибки нет. */
        val errorRes: Int? = null
    )

    sealed class Action {
        /** Повторная попытка загрузки после ошибки. */
        object Retry : Action()
    }
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: Long? = savedStateHandle.get<Long>("projectId")

    private val _state = MutableStateFlow(StatsContract.State())
    val state: StateFlow<StatsContract.State> = _state.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        val id = projectId
        if (id == null) {
            Timber.w("StatsViewModel started without projectId")
            _state.update {
                it.copy(isLoading = false, errorRes = R.string.error_load_stats)
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorRes = null) }
            
            when (val result = sessionRepository.getDailyStats(id)) {
                is Result.Success -> _state.update {
                    it.copy(dailyStats = result.data, isLoading = false)
                }
                is Result.Error -> {
                    Timber.e(result.error.cause, "Failed to load daily stats for project $id")
                    _state.update {
                        it.copy(isLoading = false, errorRes = R.string.error_load_stats)
                    }
                }
            }
        }
    }

    /**
     * Единая точка входа для действий UI (MVI).
     */
    fun onAction(action: StatsContract.Action) {
        when (action) {
            is StatsContract.Action.Retry -> retry()
        }
    }

    /**
     * Повторная попытка загрузки — вызывается через [onAction].
     * Сбрасывает errorRes и перезапускает подписку.
     */
    private fun retry() {
        _state.update { it.copy(isLoading = true, errorRes = null) }
        loadStats()
    }
}
