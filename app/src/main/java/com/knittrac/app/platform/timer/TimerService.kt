package com.knittrac.app.platform.timer
import kotlinx.coroutines.flow.Flow
interface TimerService {
    val timeFlow: Flow<Long>
    fun start()
    fun pause()
    fun reset()
}
