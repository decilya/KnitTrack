package com.knittrac.app.platform.di

import com.knittrac.app.platform.payments.NoOpPaymentManager
import com.knittrac.app.platform.payments.PaymentManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModules {

    @Binds
    @Singleton
    abstract fun bindPaymentManager(impl: NoOpPaymentManager): PaymentManager
}
