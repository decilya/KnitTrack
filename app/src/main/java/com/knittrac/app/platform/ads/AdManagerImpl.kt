package com.knittrac.app.platform.ads
import android.content.Context
import com.knittrac.app.platform.timer.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class AdManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context, 
    private val timerService: TimerService
) : AdManager {
    override var isAdsEnabled: Boolean = true
    override var isInterstitialLoaded: Boolean = false
    override var isRewardedLoaded: Boolean = false
    private var isTimerRunning: Boolean = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    init { timerService.timeFlow.onEach { seconds -> isTimerRunning = seconds > 0L }.launchIn(scope) }
    
    override fun initialize() { 
        if (isAdsEnabled) Timber.d("Yandex Ads initialization requested") 
    }
    override fun showBanner() { if (isAdsEnabled && !isTimerRunning) Timber.d("Show Banner") }
    override fun hideBanner() { Timber.d("Hide Banner") }
    override fun showInterstitial() { if (isAdsEnabled && !isTimerRunning && isInterstitialLoaded) Timber.d("Show Interstitial") }
    override fun showRewarded() { if (isAdsEnabled && !isTimerRunning && isRewardedLoaded) Timber.d("Show Rewarded") }
}
