package com.knittrac.app.platform.timer
import com.knittrac.app.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManager @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : TimerService {
    private var job: Job? = null
    private var sessionStartTime: Long = 0L
    private val _timeFlow = MutableStateFlow(0L)
    override val timeFlow: Flow<Long> = _timeFlow.asStateFlow()

    override fun start() {
        if (job?.isActive == true) return
        if (sessionStartTime == 0L) sessionStartTime = System.currentTimeMillis()
        job = CoroutineScope(dispatcher + SupervisorJob()).launch {
            while (isActive) {
                _timeFlow.value = (System.currentTimeMillis() - sessionStartTime) / 1000L
                delay(1000L)
            }
        }
    }
    override fun pause() { job?.cancel() }
    override fun reset() { job?.cancel(); sessionStartTime = 0L; _timeFlow.value = 0L }
}
