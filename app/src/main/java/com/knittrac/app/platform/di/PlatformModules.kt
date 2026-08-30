package com.knittrac.app.platform.di
import com.knittrac.app.platform.ads.AdManager
import com.knittrac.app.platform.ads.AdManagerImpl
import com.knittrac.app.platform.payments.NoOpPaymentManager
import com.knittrac.app.platform.payments.PaymentManager
import com.knittrac.app.platform.timer.TimerManager
import com.knittrac.app.platform.timer.TimerService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
@Module @InstallIn(SingletonComponent::class)
abstract class PlatformModules {
    @Binds abstract fun bindTimerService(impl: TimerManager): TimerService
    @Binds abstract fun bindAdManager(impl: AdManagerImpl): AdManager
    @Binds abstract fun bindPaymentManager(impl: NoOpPaymentManager): PaymentManager
}
