package com.knittrac.app.platform.di

import com.knittrac.app.core.common.SystemTimeProvider
import com.knittrac.app.core.common.TimeProvider
import com.knittrac.app.domain.service.TimerManager
import com.knittrac.app.platform.timer.TimerManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI-модуль для предоставления зависимостей таймера.
 * Правило: Для @Binds используется abstract class. @Singleton на классе реализации.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {

    @Binds
    @Singleton
    abstract fun bindTimeProvider(systemTimeProvider: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindTimerManager(timerManagerImpl: TimerManagerImpl): TimerManager
}