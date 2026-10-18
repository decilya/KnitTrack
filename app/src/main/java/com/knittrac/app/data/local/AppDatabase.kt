package com.knittrac.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.data.local.entity.ProjectProgressEntity
import com.knittrac.app.data.local.entity.SymbolEntity
import com.knittrac.app.data.local.entity.PatternOperationEntity
import com.knittrac.app.data.local.entity.PatternRepeatEntity

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
        SessionEntity::class,
        PatternEntity::class,
        PatternRowEntity::class,
        ProjectProgressEntity::class,
        SymbolEntity::class,
        PatternOperationEntity::class,
        PatternRepeatEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun patternDao(): PatternDao

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
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN total_rows INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN target_row_count INTEGER")
                db.execSQL(
                    "UPDATE projects SET total_rows = " +
                    "(SELECT COALESCE(SUM(row_count), 0) FROM sessions WHERE project_id = projects.id)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Санитизация старых данных перед CHECK
                db.execSQL(
                    "UPDATE projects SET name = 'Untitled' " +
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
        /**
         * Миграция с версии 4 на версию 5.
         * 
         * Добавляет таблицы схем и прогресса. Пересоздает projects для добавления FK.
         * Порядок: backup sessions -> create projects_new -> drop old -> rename -> restore sessions.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Создаем новые таблицы схем
                db.execSQL("CREATE TABLE patterns (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, technique TEXT NOT NULL, direction TEXT NOT NULL, is_reversible INTEGER NOT NULL, background_image_uri TEXT, image_offset_x REAL NOT NULL, image_offset_y REAL NOT NULL, image_scale REAL NOT NULL)")
                db.execSQL("CREATE TABLE pattern_rows (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, pattern_id INTEGER NOT NULL, row_number INTEGER NOT NULL, FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX index_pattern_rows_pattern_id ON pattern_rows (pattern_id)")
                db.execSQL("CREATE UNIQUE INDEX index_pattern_rows_pattern_id_row_number ON pattern_rows (pattern_id, row_number)")

                // 2. Бэкапим sessions (без FK, чтобы не мешал удалению projects)
                db.execSQL("CREATE TABLE sessions_backup (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, project_id INTEGER NOT NULL, start_time INTEGER NOT NULL, end_time INTEGER NOT NULL, duration_seconds INTEGER NOT NULL, row_count INTEGER NOT NULL, updated_at INTEGER NOT NULL, sync_status TEXT NOT NULL)")
                db.execSQL("INSERT INTO sessions_backup SELECT id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status FROM sessions")

                // 3. Создаем projects_new со всеми CHECK и FK
                db.execSQL("CREATE TABLE projects_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL CHECK(length(trim(name)) > 0 AND length(name) <= 100), category TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, total_time_seconds INTEGER NOT NULL CHECK(total_time_seconds >= 0), total_rows INTEGER NOT NULL DEFAULT 0 CHECK(total_rows >= 0), target_row_count INTEGER, pattern_id INTEGER, current_row_number INTEGER NOT NULL DEFAULT 0, current_pattern_row_id INTEGER, current_stitch INTEGER NOT NULL DEFAULT 0, sync_status TEXT NOT NULL, FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE SET NULL, FOREIGN KEY(current_pattern_row_id) REFERENCES pattern_rows(id) ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX index_projects_pattern_id ON projects_new (pattern_id)")
                db.execSQL("CREATE INDEX index_projects_current_pattern_row_id ON projects_new (current_pattern_row_id)")

                // 4. Переносим данные
                db.execSQL("INSERT INTO projects_new (id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, sync_status) SELECT id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, sync_status FROM projects")

                // 5. Удаляем старые таблицы (child first)
                db.execSQL("DROP TABLE sessions")
                db.execSQL("DROP TABLE projects")

                // 6. Переименовываем projects_new -> projects
                db.execSQL("ALTER TABLE projects_new RENAME TO projects")

                // 7. Восстанавливаем sessions с FK на projects
                db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, project_id INTEGER NOT NULL, start_time INTEGER NOT NULL, end_time INTEGER NOT NULL, duration_seconds INTEGER NOT NULL CHECK(duration_seconds >= 0), row_count INTEGER NOT NULL, updated_at INTEGER NOT NULL, sync_status TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX index_sessions_project_id ON sessions (project_id)")
                db.execSQL("CREATE INDEX index_sessions_start_time ON sessions (start_time)")
                db.execSQL("INSERT INTO sessions SELECT id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status FROM sessions_backup")
                db.execSQL("DROP TABLE sessions_backup")

                // 8. Создаем project_progress (после переименования projects)
                db.execSQL("CREATE TABLE project_progress (project_id INTEGER NOT NULL, pattern_row_id INTEGER NOT NULL, PRIMARY KEY(project_id, pattern_row_id), FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE, FOREIGN KEY(pattern_row_id) REFERENCES pattern_rows(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX index_project_progress_pattern_row_id ON project_progress (pattern_row_id)")

                // 9. Создаем symbols, pattern_operations, pattern_repeats
                db.execSQL("CREATE TABLE symbols (code TEXT PRIMARY KEY NOT NULL, owner_pattern_id INTEGER, display_name TEXT NOT NULL, text_glyph TEXT NOT NULL, category TEXT NOT NULL, default_column_span INTEGER NOT NULL DEFAULT 1, default_row_span INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(owner_pattern_id) REFERENCES patterns(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX index_symbols_owner_pattern_id ON symbols (owner_pattern_id)")

                db.execSQL("CREATE TABLE pattern_operations (pattern_row_id INTEGER NOT NULL, column_start INTEGER NOT NULL, symbol_code TEXT NOT NULL, back_symbol_code TEXT, color_hex TEXT, column_span_override INTEGER, row_span_override INTEGER, repeat_code TEXT, PRIMARY KEY(pattern_row_id, column_start), FOREIGN KEY(pattern_row_id) REFERENCES pattern_rows(id) ON DELETE CASCADE, FOREIGN KEY(symbol_code) REFERENCES symbols(code) ON DELETE RESTRICT, FOREIGN KEY(back_symbol_code) REFERENCES symbols(code) ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX index_pattern_operations_symbol_code ON pattern_operations (symbol_code)")
                db.execSQL("CREATE INDEX index_pattern_operations_back_symbol_code ON pattern_operations (back_symbol_code)")
                db.execSQL("CREATE INDEX index_pattern_operations_repeat_code ON pattern_operations (repeat_code)")

                db.execSQL("CREATE TABLE pattern_repeats (pattern_id INTEGER NOT NULL, code TEXT NOT NULL, stitch_count INTEGER NOT NULL, row_count INTEGER NOT NULL, PRIMARY KEY(pattern_id, code), FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE CASCADE)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "knittrac_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
