package com.knittrac.app.platform.timer

import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.service.TimerManager
import com.knittrac.app.domain.service.TimerState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManagerImpl @Inject constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : TimerManager {

    private val _elapsedTime = MutableStateFlow(0L)
    override val elapsedTime: StateFlow<Long> = _elapsedTime

    private val _state = MutableStateFlow(TimerState.IDLE)
    override val state: StateFlow<TimerState> = _state

    override var sessionStartTime: Long = 0L
        private set

    private var totalPausedDuration: Long = 0L
    private var lastPauseTime: Long = 0L
    private var job: Job? = null

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    override fun start() {
        if (job?.isActive == true) return

        val now = System.currentTimeMillis()

        if (sessionStartTime == 0L) {
            sessionStartTime = now
        } else {
            totalPausedDuration += (now - lastPauseTime)
        }

        _state.value = TimerState.RUNNING

        job = scope.launch {
            while (isActive) {
                val current = System.currentTimeMillis()
                _elapsedTime.value = (current - sessionStartTime - totalPausedDuration) / 1000L
                delay(1000L)
            }
        }
    }

    override fun pause() {
        job?.cancel()
        _state.value = TimerState.PAUSED
        lastPauseTime = System.currentTimeMillis()
    }

    override fun reset() {
        job?.cancel()
        sessionStartTime = 0L
        totalPausedDuration = 0L
        lastPauseTime = 0L
        _elapsedTime.value = 0L
        _state.value = TimerState.IDLE
    }
}