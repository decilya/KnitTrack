package com.knittrac.app.data.di

import android.content.Context
import androidx.room.Room
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.PatternDao
import com.knittrac.app.data.local.PatternRepeatDao
import com.knittrac.app.data.local.SymbolDao
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "knittrac_database"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9
            )
            .addCallback(AppDatabase.DATABASE_CALLBACK)
            .build()
    }

    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun providePatternDao(database: AppDatabase): PatternDao {
        return database.patternDao()
    }

    @Provides
    @Singleton
    fun provideSymbolDao(database: AppDatabase): SymbolDao {
        return database.symbolDao()
    }

    @Provides
    @Singleton
    fun providePatternRepeatDao(database: AppDatabase): PatternRepeatDao {
        return database.patternRepeatDao()
    }
}
