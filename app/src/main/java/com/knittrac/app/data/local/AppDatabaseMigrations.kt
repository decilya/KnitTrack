package com.knittrac.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Все миграции и DDL-скрипты базы данных Room.
 *
 * Вынесены из [AppDatabase] для соблюдения SRP (Single Responsibility Principle).
 */
object AppDatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Пустая миграция.
        }
    }

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
            db.execSQL("UPDATE projects SET name = 'Untitled' WHERE length(trim(name)) = 0")
            db.execSQL("UPDATE projects SET total_time_seconds = 0 WHERE total_time_seconds < 0")
            db.execSQL("UPDATE sessions SET duration_seconds = 0 WHERE duration_seconds < 0")

            migrate2To3CreateProjectsTable(db)
            migrate2To3CreateSessionsTable(db)

            db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_project_id ON sessions (project_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_start_time ON sessions (start_time)")
        }
    }

    /**
     * Пересоздает таблицу `projects` с CHECK constraints (миграция 2->3).
     */
    private fun migrate2To3CreateProjectsTable(db: SupportSQLiteDatabase) {
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
    }

    /**
     * Пересоздает таблицу `sessions` с CHECK и FK (миграция 2->3).
     */
    private fun migrate2To3CreateSessionsTable(db: SupportSQLiteDatabase) {
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
    }

    // ==========================================
    // Хелперы для MIGRATION_4_5
    // ==========================================

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrate4To5CreatePatternTables(db)
            migrate4To5MigrateSessionsAndProjects(db)
            migrate4To5CreateLegendTables(db)
        }
    }

    /**
     * Создает таблицы схем (`patterns`, `pattern_rows`) для миграции 4->5.
     */
    private fun migrate4To5CreatePatternTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE patterns (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, technique TEXT NOT NULL, direction TEXT NOT NULL, is_reversible INTEGER NOT NULL, background_image_uri TEXT, image_offset_x REAL NOT NULL, image_offset_y REAL NOT NULL, image_scale REAL NOT NULL)")
        db.execSQL("CREATE TABLE pattern_rows (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, pattern_id INTEGER NOT NULL, row_number INTEGER NOT NULL, FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_pattern_rows_pattern_id ON pattern_rows (pattern_id)")
        db.execSQL("CREATE UNIQUE INDEX index_pattern_rows_pattern_id_row_number ON pattern_rows (pattern_id, row_number)")
    }

    /**
     * Переносит данные из старых таблиц в новые (миграция 4->5):
     * бэкап `sessions`, пересоздание `projects` с FK, восстановление `sessions`.
     */
    private fun migrate4To5MigrateSessionsAndProjects(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE sessions_backup (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, project_id INTEGER NOT NULL, start_time INTEGER NOT NULL, end_time INTEGER NOT NULL, duration_seconds INTEGER NOT NULL, row_count INTEGER NOT NULL, updated_at INTEGER NOT NULL, sync_status TEXT NOT NULL)")
        db.execSQL("INSERT INTO sessions_backup SELECT id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status FROM sessions")
        db.execSQL(CREATE_PROJECTS_WITH_CHECK)
        db.execSQL(CREATE_INDEX_PROJECTS_PATTERN_ID)
        db.execSQL(CREATE_INDEX_PROJECTS_CURRENT_ROW)
        db.execSQL("INSERT INTO projects_new (id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, sync_status) SELECT id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, sync_status FROM projects")
        db.execSQL("DROP TABLE sessions")
        db.execSQL("DROP TABLE projects")
        db.execSQL("ALTER TABLE projects_new RENAME TO projects")
        db.execSQL(CREATE_SESSIONS_WITH_CHECK)
        db.execSQL(CREATE_INDEX_SESSIONS_PROJECT_ID)
        db.execSQL(CREATE_INDEX_SESSIONS_START_TIME)
        db.execSQL("INSERT INTO sessions SELECT id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status FROM sessions_backup")
        db.execSQL("DROP TABLE sessions_backup")
        db.execSQL(CREATE_PROJECT_PROGRESS)
        db.execSQL(CREATE_INDEX_PROJECT_PROGRESS_ROW)
    }

    /**
     * Создает таблицы легенды (`symbols`, `pattern_operations`, `pattern_repeats`)
     * для миграции 4->5.
     */
    private fun migrate4To5CreateLegendTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE symbols (code TEXT PRIMARY KEY NOT NULL, owner_pattern_id INTEGER, display_name TEXT NOT NULL, text_glyph TEXT NOT NULL, category TEXT NOT NULL, default_column_span INTEGER NOT NULL DEFAULT 1, default_row_span INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(owner_pattern_id) REFERENCES patterns(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_symbols_owner_pattern_id ON symbols (owner_pattern_id)")
        db.execSQL("CREATE TABLE pattern_operations (pattern_row_id INTEGER NOT NULL, column_start INTEGER NOT NULL, symbol_code TEXT NOT NULL, back_symbol_code TEXT, color_hex TEXT, column_span_override INTEGER, row_span_override INTEGER, repeat_code TEXT, PRIMARY KEY(pattern_row_id, column_start), FOREIGN KEY(pattern_row_id) REFERENCES pattern_rows(id) ON DELETE CASCADE, FOREIGN KEY(symbol_code) REFERENCES symbols(code) ON DELETE RESTRICT, FOREIGN KEY(back_symbol_code) REFERENCES symbols(code) ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX index_pattern_operations_symbol_code ON pattern_operations (symbol_code)")
        db.execSQL("CREATE INDEX index_pattern_operations_back_symbol_code ON pattern_operations (back_symbol_code)")
        db.execSQL("CREATE INDEX index_pattern_operations_repeat_code ON pattern_operations (repeat_code)")
        db.execSQL("CREATE TABLE pattern_repeats (pattern_id INTEGER NOT NULL, code TEXT NOT NULL, stitch_count INTEGER NOT NULL, row_count INTEGER NOT NULL, PRIMARY KEY(pattern_id, code), FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE CASCADE)")
    }

    // ==========================================
    // DDL константы
    // ==========================================

    internal const val CREATE_PROJECTS_WITH_CHECK =
        "CREATE TABLE projects_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL CHECK(length(trim(name)) > 0 AND length(name) <= 100), category TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, total_time_seconds INTEGER NOT NULL CHECK(total_time_seconds >= 0), total_rows INTEGER NOT NULL DEFAULT 0 CHECK(total_rows >= 0), target_row_count INTEGER, pattern_id INTEGER, current_row_number INTEGER NOT NULL DEFAULT 0, current_pattern_row_id INTEGER, current_stitch INTEGER NOT NULL DEFAULT 0, sync_status TEXT NOT NULL, FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE SET NULL, FOREIGN KEY(current_pattern_row_id) REFERENCES pattern_rows(id) ON DELETE SET NULL)"

    internal const val CREATE_INDEX_PROJECTS_PATTERN_ID =
        "CREATE INDEX index_projects_pattern_id ON projects_new (pattern_id)"

    internal const val CREATE_INDEX_PROJECTS_CURRENT_ROW =
        "CREATE INDEX index_projects_current_pattern_row_id ON projects_new (current_pattern_row_id)"

    internal const val CREATE_SESSIONS_WITH_CHECK =
        "CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, project_id INTEGER NOT NULL, start_time INTEGER NOT NULL, end_time INTEGER NOT NULL, duration_seconds INTEGER NOT NULL CHECK(duration_seconds >= 0), row_count INTEGER NOT NULL, updated_at INTEGER NOT NULL, sync_status TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)"

    internal const val CREATE_INDEX_SESSIONS_PROJECT_ID =
        "CREATE INDEX index_sessions_project_id ON sessions (project_id)"

    internal const val CREATE_INDEX_SESSIONS_START_TIME =
        "CREATE INDEX index_sessions_start_time ON sessions (start_time)"

    internal const val CREATE_PROJECT_PROGRESS =
        "CREATE TABLE project_progress (project_id INTEGER NOT NULL, pattern_row_id INTEGER NOT NULL, PRIMARY KEY(project_id, pattern_row_id), FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE, FOREIGN KEY(pattern_row_id) REFERENCES pattern_rows(id) ON DELETE CASCADE)"

    internal const val CREATE_INDEX_PROJECT_PROGRESS_ROW =
        "CREATE INDEX index_project_progress_pattern_row_id ON project_progress (pattern_row_id)"

    internal const val CREATE_PROJECTS_V10 =
        "CREATE TABLE projects_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL CHECK(length(trim(name)) > 0 AND length(name) <= 100), category TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, total_time_seconds INTEGER NOT NULL CHECK(total_time_seconds >= 0), total_rows INTEGER NOT NULL DEFAULT 0 CHECK(total_rows >= 0), target_row_count INTEGER, pattern_id INTEGER, current_pattern_row_id INTEGER, current_stitch INTEGER NOT NULL DEFAULT 0, sync_status TEXT NOT NULL, FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE SET NULL, FOREIGN KEY(current_pattern_row_id) REFERENCES pattern_rows(id) ON DELETE SET NULL)"

    internal const val CREATE_TRIGGER_PROGRESS_INSERT_V10 =
        "CREATE TRIGGER trg_progress_check_insert " +
            "BEFORE INSERT ON projects BEGIN " +
            "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND pattern_id IS NEW.pattern_id); " +
            "SELECT RAISE(ABORT, 'Петли без ряда невозможны') " +
            "WHERE NEW.current_stitch > 0 AND NEW.current_pattern_row_id IS NULL; " +
            "END"

    internal const val CREATE_TRIGGER_PROGRESS_UPDATE_V10 =
        "CREATE TRIGGER trg_progress_check_update " +
            "BEFORE UPDATE OF current_pattern_row_id, current_stitch, pattern_id " +
            "ON projects BEGIN " +
            "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND pattern_id IS NEW.pattern_id); " +
            "SELECT RAISE(ABORT, 'Петли без ряда невозможны') " +
            "WHERE NEW.current_stitch > 0 AND NEW.current_pattern_row_id IS NULL; " +
            "END"

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE sessions_backup (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, project_id INTEGER NOT NULL, start_time INTEGER NOT NULL, end_time INTEGER NOT NULL, duration_seconds INTEGER NOT NULL, row_count INTEGER NOT NULL, updated_at INTEGER NOT NULL, sync_status TEXT NOT NULL)")
            db.execSQL("INSERT INTO sessions_backup SELECT id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status FROM sessions")

            db.execSQL("CREATE TABLE project_progress_backup (project_id INTEGER NOT NULL, pattern_row_id INTEGER NOT NULL, PRIMARY KEY(project_id, pattern_row_id))")
            db.execSQL("INSERT INTO project_progress_backup SELECT project_id, pattern_row_id FROM project_progress")

            db.execSQL("DROP TABLE sessions")
            db.execSQL("DROP TABLE project_progress")

            db.execSQL(CREATE_PROJECTS_V10)
            db.execSQL("DROP INDEX IF EXISTS index_projects_pattern_id")
            db.execSQL("DROP INDEX IF EXISTS index_projects_current_pattern_row_id")
            db.execSQL(CREATE_INDEX_PROJECTS_PATTERN_ID)
            db.execSQL(CREATE_INDEX_PROJECTS_CURRENT_ROW)

            db.execSQL("INSERT INTO projects_new (id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, pattern_id, current_pattern_row_id, current_stitch, sync_status) SELECT id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, pattern_id, current_pattern_row_id, current_stitch, sync_status FROM projects")

            db.execSQL("DROP TABLE projects")
            db.execSQL("ALTER TABLE projects_new RENAME TO projects")

            db.execSQL(CREATE_SESSIONS_WITH_CHECK)
            db.execSQL(CREATE_INDEX_SESSIONS_PROJECT_ID)
            db.execSQL(CREATE_INDEX_SESSIONS_START_TIME)
            db.execSQL("INSERT INTO sessions SELECT id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status FROM sessions_backup")
            db.execSQL("DROP TABLE sessions_backup")

            db.execSQL(CREATE_PROJECT_PROGRESS)
            db.execSQL(CREATE_INDEX_PROJECT_PROGRESS_ROW)
            db.execSQL("INSERT INTO project_progress SELECT project_id, pattern_row_id FROM project_progress_backup")
            db.execSQL("DROP TABLE project_progress_backup")

            db.execSQL(CREATE_TRIGGER_PROGRESS_INSERT_V10)
            db.execSQL(CREATE_TRIGGER_PROGRESS_UPDATE_V10)
        }
    }

    internal const val CREATE_TRIGGER_PROGRESS_INSERT =
        "CREATE TRIGGER trg_progress_check_insert " +
            "BEFORE INSERT ON projects BEGIN " +
            "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND pattern_id IS NEW.pattern_id); " +
            "SELECT RAISE(ABORT, 'Номер ряда не соответствует денормализации') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND row_number = NEW.current_row_number); " +
            "SELECT RAISE(ABORT, 'Прогресс без ряда должен иметь номер 0') " +
            "WHERE NEW.current_pattern_row_id IS NULL " +
            "AND NEW.current_row_number != 0; " +
            "END"

    internal const val CREATE_TRIGGER_PROGRESS_INSERT_V8 =
        "CREATE TRIGGER trg_progress_check_insert " +
            "BEFORE INSERT ON projects BEGIN " +
            "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND pattern_id IS NEW.pattern_id); " +
            "SELECT RAISE(ABORT, 'Номер ряда не соответствует денормализации') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND row_number = NEW.current_row_number); " +
            "SELECT RAISE(ABORT, 'Прогресс без ряда должен иметь номер 0') " +
            "WHERE NEW.current_pattern_row_id IS NULL " +
            "AND NEW.current_row_number != 0; " +
            "SELECT RAISE(ABORT, 'Петли без ряда невозможны') " +
            "WHERE NEW.current_stitch > 0 AND NEW.current_pattern_row_id IS NULL; " +
            "END"

    internal const val CREATE_TRIGGER_PROGRESS_UPDATE_V8 =
        "CREATE TRIGGER trg_progress_check_update " +
            "BEFORE UPDATE OF current_pattern_row_id, current_row_number, " +
            "current_stitch, pattern_id ON projects BEGIN " +
            "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND pattern_id IS NEW.pattern_id); " +
            "SELECT RAISE(ABORT, 'Номер ряда не соответствует денормализации') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND row_number = NEW.current_row_number); " +
            "SELECT RAISE(ABORT, 'Прогресс без ряда должен иметь номер 0') " +
            "WHERE NEW.current_pattern_row_id IS NULL " +
            "AND NEW.current_row_number != 0; " +
            "SELECT RAISE(ABORT, 'Петли без ряда невозможны') " +
            "WHERE NEW.current_stitch > 0 AND NEW.current_pattern_row_id IS NULL; " +
            "END"

    internal const val CREATE_TRIGGER_PROGRESS_UPDATE =
        "CREATE TRIGGER trg_progress_check_update " +
            "BEFORE UPDATE OF current_pattern_row_id, current_row_number, " +
            "current_stitch, pattern_id ON projects BEGIN " +
            "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND pattern_id IS NEW.pattern_id); " +
            "SELECT RAISE(ABORT, 'Номер ряда не соответствует денормализации') " +
            "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_rows " +
            "WHERE id = NEW.current_pattern_row_id " +
            "AND row_number = NEW.current_row_number); " +
            "SELECT RAISE(ABORT, 'Прогресс без ряда должен иметь номер 0') " +
            "WHERE NEW.current_pattern_row_id IS NULL " +
            "AND NEW.current_row_number != 0; " +
            "END"

    internal const val CREATE_TRIGGER_REPEAT_INSERT_V9 =
        "CREATE TRIGGER trg_repeat_code_check_insert " +
            "BEFORE INSERT ON pattern_operations BEGIN " +
            "SELECT RAISE(ABORT, 'Раппорт не принадлежит схеме') " +
            "WHERE NEW.repeat_code IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_repeats pr " +
            "INNER JOIN pattern_rows r ON r.pattern_id = pr.pattern_id " +
            "WHERE r.id = NEW.pattern_row_id " +
            "AND pr.code = NEW.repeat_code); " +
            "END"

    internal const val CREATE_TRIGGER_REPEAT_UPDATE_V9 =
        "CREATE TRIGGER trg_repeat_code_check_update " +
            "BEFORE UPDATE OF repeat_code ON pattern_operations " +
            "WHEN OLD.repeat_code IS NOT NEW.repeat_code " +
            "BEGIN " +
            "SELECT RAISE(ABORT, 'Раппорт не принадлежит схеме') " +
            "WHERE NEW.repeat_code IS NOT NULL AND NOT EXISTS (" +
            "SELECT 1 FROM pattern_repeats pr " +
            "INNER JOIN pattern_rows r ON r.pattern_id = pr.pattern_id " +
            "WHERE r.id = NEW.pattern_row_id " +
            "AND pr.code = NEW.repeat_code); " +
            "END"

    internal const val DEFAULT_SYMBOLS_INSERT = """
        INSERT OR IGNORE INTO symbols
        (code, owner_pattern_id, display_name, text_glyph, category, default_column_span, default_row_span)
        VALUES
        ('K', NULL, 'Лицевая', 'V', 'STANDARD', 1, 1),
        ('P', NULL, 'Изнаночная', '-', 'STANDARD', 1, 1),
        ('O', NULL, 'Накид', 'o', 'STANDARD', 1, 1),
        ('K2tog', NULL, 'Две вместе лицевой', '/', 'STANDARD', 1, 1),
        ('SSK', NULL, 'Две вместе с наклоном влево', '\', 'STANDARD', 1, 1),
        ('SL', NULL, 'Снять петлю', '▢', 'STANDARD', 1, 1)
    """

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "UPDATE projects SET current_pattern_row_id = NULL, current_row_number = 0 " +
                    "WHERE current_pattern_row_id IS NOT NULL AND (" +
                    "pattern_id IS NULL OR NOT EXISTS (" +
                    "SELECT 1 FROM pattern_rows " +
                    "WHERE id = current_pattern_row_id " +
                    "AND pattern_id = projects.pattern_id))"
            )
            db.execSQL(
                "UPDATE projects SET current_row_number = (" +
                    "SELECT row_number FROM pattern_rows " +
                    "WHERE id = current_pattern_row_id) " +
                    "WHERE current_pattern_row_id IS NOT NULL " +
                    "AND current_row_number != (" +
                    "SELECT row_number FROM pattern_rows " +
                    "WHERE id = current_pattern_row_id)"
            )
            db.execSQL(
                "UPDATE projects SET current_row_number = 0 " +
                    "WHERE current_pattern_row_id IS NULL " +
                    "AND current_row_number != 0"
            )
            db.execSQL(CREATE_TRIGGER_PROGRESS_INSERT)
            db.execSQL(CREATE_TRIGGER_PROGRESS_UPDATE)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "UPDATE projects SET current_stitch = 0 " +
                "WHERE current_pattern_row_id IS NULL AND current_stitch > 0"
            )
            db.execSQL("DROP TRIGGER IF EXISTS trg_progress_check_insert")
            db.execSQL("DROP TRIGGER IF EXISTS trg_progress_check_update")
            db.execSQL(CREATE_TRIGGER_PROGRESS_INSERT_V8)
            db.execSQL(CREATE_TRIGGER_PROGRESS_UPDATE_V8)
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(DEFAULT_SYMBOLS_INSERT)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "UPDATE pattern_operations SET repeat_code = NULL " +
                "WHERE repeat_code IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_repeats pr " +
                "INNER JOIN pattern_rows r ON r.pattern_id = pr.pattern_id " +
                "WHERE r.id = pattern_operations.pattern_row_id " +
                "AND pr.code = pattern_operations.repeat_code)"
            )
            db.execSQL("DROP TRIGGER IF EXISTS trg_repeat_code_check_insert")
            db.execSQL("DROP TRIGGER IF EXISTS trg_repeat_code_check_update")
            db.execSQL(CREATE_TRIGGER_REPEAT_INSERT_V9)
            db.execSQL(CREATE_TRIGGER_REPEAT_UPDATE_V9)
        }
    }
}
