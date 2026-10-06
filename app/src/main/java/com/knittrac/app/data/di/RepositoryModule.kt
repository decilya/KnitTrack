package com.knittrac.app.data.di

import com.knittrac.app.data.repository.DataExporterImpl
import com.knittrac.app.data.repository.DataImporterImpl
import com.knittrac.app.data.repository.ProjectRepositoryImpl
import com.knittrac.app.data.repository.SessionRepositoryImpl
import com.knittrac.app.domain.repository.DataExporter
import com.knittrac.app.domain.repository.DataImporter
import com.knittrac.app.domain.repository.ProjectRepository
import com.knittrac.app.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt-модуль для привязки интерфейсов репозиториев к их реализациям.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun bindDataExporter(impl: DataExporterImpl): DataExporter

    @Binds
    abstract fun bindDataImporter(impl: DataImporterImpl): DataImporter
}
