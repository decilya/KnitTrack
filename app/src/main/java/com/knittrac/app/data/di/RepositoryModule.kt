package com.knittrac.app.data.di

import com.knittrac.app.data.repository.DataExporterImpl
import com.knittrac.app.data.repository.DataImporterImpl
import com.knittrac.app.data.repository.ProjectRepositoryImpl
import com.knittrac.app.data.repository.SessionRepositoryImpl
import com.knittrac.app.domain.repository.DataExporter
import com.knittrac.app.domain.repository.BackupRepository
import com.knittrac.app.data.repository.BackupRepositoryImpl
import com.knittrac.app.domain.repository.DataImporter
import com.knittrac.app.domain.repository.ProjectRepository
import com.knittrac.app.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль для привязки интерфейсов репозиториев к их реализациям.
 *
 * Все биндинги @Singleton: репозитории держат DAO и диспетчер,
 * пересоздание на каждый запрос было бы лишним аллоцированием.
 * Scope объявлен на @Binds, а не на impl-классах — это каноничнее по DIP:
 * реализация не знает, что она Singleton, scope определяет DI-контракт.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** Связывает [ProjectRepository] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    /** Связывает [SessionRepository] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    /** Связывает [BackupRepository] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    /** Связывает [DataExporter] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindDataExporter(impl: DataExporterImpl): DataExporter

    /** Связывает [DataImporter] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindDataImporter(impl: DataImporterImpl): DataImporter
}
