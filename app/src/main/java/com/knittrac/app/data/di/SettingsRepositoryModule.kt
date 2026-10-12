package com.knittrac.app.data.di

import com.knittrac.app.data.repository.SettingsRepository
import com.knittrac.app.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль для привязки [SettingsRepository] к реализации.
 *
 * @Singleton на @Binds — каноничнее, чем на impl-классе: scope
 * определяется контрактом DI, реализация о нём не знает.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsRepositoryModule {

    /** Связывает [SettingsRepository] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
