package com.knittrac.app.platform.di

import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.platform.payments.NoOpPaymentManager
import com.knittrac.app.platform.payments.PaymentManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModules {

    @Binds
    @Singleton
    abstract fun bindPaymentManager(impl: NoOpPaymentManager): PaymentManager
}

@Module
@InstallIn(SingletonComponent::class)
object CoroutineDispatchersModule {
    
    @Provides
    @IoDispatcher
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
