package com.knittrac.app.data.di
import com.knittrac.app.data.repository.ProjectRepositoryImpl
import com.knittrac.app.data.repository.SessionRepositoryImpl
import com.knittrac.app.data.repository.SyncRepositoryImpl
import com.knittrac.app.domain.repository.ProjectRepository
import com.knittrac.app.domain.repository.SessionRepository
import com.knittrac.app.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository
    @Binds abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
    @Binds abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
