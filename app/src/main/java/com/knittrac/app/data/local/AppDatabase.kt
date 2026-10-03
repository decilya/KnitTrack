package com.knittrac.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session

/**
 * Главная база данных приложения Room.
 * version = 1: начальная версия схемы.
 * exportSchema = false: отключаем экспорт схемы в папку (для простоты на данном этапе).
 */
@Database(
    entities = [Project::class, Session::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "knittrac_database"
                )
                // В будущем здесь будут миграции (.addMigrations)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
