package com.knittrack.app.data.di

import com.knittrack.app.data.repository.DataExporterImpl
import com.knittrack.app.data.repository.DataImporterImpl
import com.knittrack.app.data.repository.PatternRepositoryImpl
import com.knittrack.app.data.repository.ProjectRepositoryImpl
import com.knittrack.app.data.repository.SessionRepositoryImpl
import com.knittrack.app.domain.repository.DataExporter
import com.knittrack.app.domain.repository.BackupRepository
import com.knittrack.app.data.repository.BackupRepositoryImpl
import com.knittrack.app.domain.repository.DataImporter
import com.knittrack.app.domain.repository.PatternImageExporter
import com.knittrack.app.domain.repository.PdfPatternExporter
import com.knittrack.app.data.renderer.PdfPatternRenderer
import com.knittrack.app.data.renderer.SvgPatternRenderer

import com.knittrack.app.domain.repository.PatternRepository
import com.knittrack.app.domain.repository.ProjectRepository
import com.knittrack.app.domain.repository.SessionRepository
import com.knittrack.app.core.common.ErrorStringProvider
import com.knittrack.app.data.local.AndroidErrorStringProvider
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

    @Binds
    @Singleton
    abstract fun bindPatternRepository(impl: PatternRepositoryImpl): PatternRepository

    /** Связывает [BackupRepository] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    /** Связывает [DataExporter] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindDataExporter(impl: DataExporterImpl): DataExporter

    /** Связывает [ErrorStringProvider] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindErrorStringProvider(impl: AndroidErrorStringProvider): ErrorStringProvider

    /** Связывает [DataImporter] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindDataImporter(impl: DataImporterImpl): DataImporter

    /** Связывает [PatternImageExporter] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindPatternImageExporter(impl: SvgPatternRenderer): PatternImageExporter

    /** Связывает [PdfPatternExporter] с реализацией. */
    @Binds
    @Singleton
    abstract fun bindPdfPatternExporter(impl: PdfPatternRenderer): PdfPatternExporter

}
