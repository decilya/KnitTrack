package com.knittrac.app.platform.di

import com.knittrac.app.platform.notification.AndroidNotificationPermissionChecker
import com.knittrac.app.platform.notification.NotificationPermissionChecker
import com.knittrac.app.platform.payments.NoOpPaymentManager
import com.knittrac.app.platform.payments.PaymentManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI-модуль platform-слоя: подвязка интерфейсов к реализациям.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModules {

    @Binds
    @Singleton
    abstract fun bindPaymentManager(impl: NoOpPaymentManager): PaymentManager

    @Binds
    @Singleton
    abstract fun bindNotificationPermissionChecker(
        impl: AndroidNotificationPermissionChecker
    ): NotificationPermissionChecker
}
