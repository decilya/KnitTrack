package com.knittrac.app.data.di

import com.knittrac.app.data.local.AppDatabaseMigrations
import com.knittrac.app.data.local.DatabaseCallback

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
                AppDatabaseMigrations.MIGRATION_1_2, AppDatabaseMigrations.MIGRATION_2_3,
                AppDatabaseMigrations.MIGRATION_3_4, AppDatabaseMigrations.MIGRATION_4_5,
                AppDatabaseMigrations.MIGRATION_5_6, AppDatabaseMigrations.MIGRATION_6_7,
                AppDatabaseMigrations.MIGRATION_7_8, AppDatabaseMigrations.MIGRATION_8_9,
                AppDatabaseMigrations.MIGRATION_9_10
            )
            .addCallback(DatabaseCallback)
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
