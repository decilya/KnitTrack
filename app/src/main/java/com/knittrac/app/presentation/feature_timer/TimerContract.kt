package com.knittrac.app.presentation.feature_timer

import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.service.TimerState

/**
 * MVI-контракт экрана таймера.
 *
 * State — единый снапшот для UI. Intent — действия пользователя.
 * Effect — одноразовые события (snackbar, навигация, запрос permission).
 */
object TimerContract {

    data class State(
        val elapsedTime: Long = 0L,
        val status: TimerState = TimerState.IDLE,
        val projectId: Long? = null,
        val startTimestamp: Long = 0L,
        val rowCount: Int = 0,
        val targetRowCount: Int? = null,
        val projects: List<Project> = emptyList(),
        /**
         * Флаг активного сохранения сессии.
         *
         * Защищает от двойного тапа по кнопке Save: пока true — повторный
         * [Intent.SaveSession] игнорируется, кнопка в UI disabled.
         */
        val isSaving: Boolean = false,

        /**
         * Номер текущего ряда по схеме (1-based), `null` — прогресс по схеме
         * ещё не начат (`currentPatternRowId` у проекта = null). Источник:
         * [com.knittrac.app.domain.entity.Project.currentRowNumber],
         * обновляется после успешного [Intent.MarkNextPatternRow].
         *
         * ВАЖНО: не путать с [rowCount] — это локальный счётчик сессии
         * (в `SavedStateHandle`, идёт в `SaveSessionParams.rowCount`).
         * По аналогии с KnitCompanion: One-Tap Marker (это поле) и
         * Simple Counter ([rowCount]) — независимые счётчики.
         */
        val currentPatternRowNumber: Int? = null,

        /**
         * Флаг активной отметки следующего ряда схемы.
         *
         * Защищает от двойного тапа: пока `true` — повторный
         * [Intent.MarkNextPatternRow] игнорируется, кнопка в UI disabled.
         */
        val isMarkingRow: Boolean = false
    )

    sealed class Intent {
        object Start : Intent()
        object Pause : Intent()
        object Reset : Intent()
        data class SelectProject(val projectId: Long) : Intent()
        object SaveSession : Intent()
        object IncrementRow : Intent()
        object DecrementRow : Intent()

        /**
         * Отметить следующий ряд схемы как связанный.
         *
         * В отличие от [IncrementRow] (локальный счётчик сессии),
         * обновляет `Project.currentPatternRowId` в БД через
         * [com.knittrac.app.domain.service.MarkNextRowUseCase].
         */
        object MarkNextPatternRow : Intent()
    }

    sealed class Effect {
        data class ShowError(@androidx.annotation.StringRes val messageResId: Int) : Effect()
        object SessionSaved : Effect()
        object NavigateToProjects : Effect()
    }
}
