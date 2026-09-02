package com.knittrac.app.presentation.feature_timer

import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.service.TimerState

object TimerContract {
    data class State(
        val elapsedTime: Long = 0L,
        val status: TimerState = TimerState.IDLE,
        val projectId: Long? = null,
        val startTimestamp: Long = 0L,
        val rowCount: Int = 0,
        val projects: List<Project> = emptyList()
    )

    sealed class Intent {
        object Start : Intent()
        object Pause : Intent()
        object Reset : Intent()
        data class SelectProject(val projectId: Long) : Intent()
        object SaveSession : Intent()
        data class UpdateRowCount(val count: Int) : Intent()
    }

    sealed class Effect {
        data class ShowError(val message: String) : Effect()
        object SessionSaved : Effect()
        object NavigateToProjects : Effect()
    }
}