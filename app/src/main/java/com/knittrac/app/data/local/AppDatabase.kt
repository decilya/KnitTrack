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
 * 2. version = 3: миграция 2->3 добавляет CHECK constraints для
 *    защиты инвариантов Value Objects (ProjectName, Duration).
 * 3. Каждая миграция должна вручную переносить CHECK constraints
 *    в новую CREATE TABLE — Room их не видит и не сохраняет.
 *    См. docs/architecture/vo-pattern.md.
 */
@Database(
    entities = [
        ProjectEntity::class,
        SessionEntity::class
    ],
    version = 3,
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
         * Пустая — служит маркером первой миграции.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Пустая миграция.
            }
        }

        /**
         * Миграция с версии 2 на версию 3.
         *
         * Зачем: добавляет CHECK constraints на level БД для защиты
         * инвариантов Value Objects ProjectName и Duration.
         *
         * SQLite не поддерживает ALTER TABLE ADD CONSTRAINT,
         * поэтому таблицы пересоздаются через CREATE + INSERT SELECT + DROP + RENAME.
         *
         * Перед переносом данных — санитизация: пустые имена заменяются
         * на «Без названия», отрицательные числа на 0. Иначе CHECK упадёт.
         *
         * ВАЖНО: структура новых таблиц ДОЛЖНА точно совпадать с тем,
         * что генерирует Room из @Entity (кроме CHECK, который Room не видит).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Санитизация старых данных перед CHECK
                db.execSQL(
                    "UPDATE projects SET name = 'Без названия' " +
                    "WHERE length(trim(name)) = 0"
                )
                db.execSQL(
                    "UPDATE projects SET total_time_seconds = 0 " +
                    "WHERE total_time_seconds < 0"
                )
                db.execSQL(
                    "UPDATE sessions SET duration_seconds = 0 " +
                    "WHERE duration_seconds < 0"
                )

                // 2. Пересоздание projects с CHECK
                db.execSQL(
                    """
                    CREATE TABLE projects_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                            CHECK(length(trim(name)) > 0 AND length(name) <= 100),
                        category TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        total_time_seconds INTEGER NOT NULL
                            CHECK(total_time_seconds >= 0),
                        sync_status TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO projects_new " +
                    "SELECT id, name, category, created_at, updated_at, " +
                    "total_time_seconds, sync_status FROM projects"
                )
                db.execSQL("DROP TABLE projects")
                db.execSQL("ALTER TABLE projects_new RENAME TO projects")

                // 3. Пересоздание sessions с CHECK и FK
                db.execSQL(
                    """
                    CREATE TABLE sessions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        project_id INTEGER NOT NULL,
                        start_time INTEGER NOT NULL,
                        end_time INTEGER NOT NULL,
                        duration_seconds INTEGER NOT NULL
                            CHECK(duration_seconds >= 0),
                        row_count INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        sync_status TEXT NOT NULL,
                        FOREIGN KEY(project_id) REFERENCES projects(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO sessions_new " +
                    "SELECT id, project_id, start_time, end_time, " +
                    "duration_seconds, row_count, updated_at, sync_status " +
                    "FROM sessions"
                )
                db.execSQL("DROP TABLE sessions")
                db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")

                // 4. Индексы sessions (Room ожидает их наличия)
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sessions_project_id " +
                    "ON sessions (project_id)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sessions_start_time " +
                    "ON sessions (start_time)"
                )
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
