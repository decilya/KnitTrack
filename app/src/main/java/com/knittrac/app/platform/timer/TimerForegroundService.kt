package com.knittrac.app.platform.timer
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.knittrac.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
@AndroidEntryPoint
class TimerForegroundService : Service() {
    @Inject lateinit var timerService: TimerService
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                startForeground(1, NotificationCompat.Builder(this, "timer_channel").setContentTitle("KnitTrac Timer").setSmallIcon(R.drawable.ic_launcher_foreground).build())
                serviceScope.launch { timerService.start() }
            }
            "STOP" -> {
                timerService.reset()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { super.onDestroy(); serviceScope.cancel() }
}
