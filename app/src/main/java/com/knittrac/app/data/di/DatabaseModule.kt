package com.knittrac.app.data.di
import android.content.Context
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.dao.ProjectDao
import com.knittrac.app.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.getInstance(context)
    @Provides fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()
    @Provides fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()
}
