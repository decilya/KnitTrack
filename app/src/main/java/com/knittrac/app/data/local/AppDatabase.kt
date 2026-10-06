package com.knittrac.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.data.local.entity.SessionEntity

/**
 * Главная конфигурация базы данных приложения Room.
 * 
 * ВАЖНО: 
 * 1. Все классы, аннотированные @Entity, перечислены в `entities`.
 * 2. version = 2: версия увеличена для фиксации добавления миграции.
 * 3. Добавлена явная миграция 1->2, чтобы избежать потери данных пользователей.
 */
@Database(
    entities = [
        ProjectEntity::class,
        SessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Миграция с версии 1 на версию 2.
         * Сейчас она пустая, так как схема не менялась, но её наличие 
         * гарантирует, что Room не удалит базу данных при будущем обновлении версии.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Пустая миграция. Здесь будут SQL-запросы ALTER TABLE 
                // при добавлении новых колонок в будущем.
            }
        }

        /**
         * Реализует паттерн Singleton для базы данных.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "knittrac_database"
                )
                // Явно добавляем миграцию для безопасного обновления схемы
                .addMigrations(MIGRATION_1_2)
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
