package com.knittrack.app.platform.di

import com.knittrack.app.platform.notification.AndroidNotificationPermissionChecker
import com.knittrack.app.platform.notification.NotificationPermissionChecker
import com.knittrack.app.platform.payments.NoOpPaymentService
import com.knittrack.app.platform.payments.PaymentService
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
    abstract fun bindPaymentService(impl: NoOpPaymentService): PaymentService

    @Binds
    @Singleton
    abstract fun bindNotificationPermissionChecker(
        impl: AndroidNotificationPermissionChecker
    ): NotificationPermissionChecker
}
