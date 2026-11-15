package com.knittrack.app.platform.di

import com.knittrack.app.core.common.TimeProvider
import com.knittrack.app.domain.service.TimerService
import com.knittrack.app.platform.timer.SystemTimeProvider
import com.knittrack.app.platform.timer.TimerServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI-модуль для предоставления зависимостей таймера.
 * Правило: для @Binds используется abstract class, @Singleton на реализации.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {

    @Binds
    @Singleton
    abstract fun bindTimeProvider(systemTimeProvider: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindTimerService(timerManagerImpl: TimerServiceImpl): TimerService
}
