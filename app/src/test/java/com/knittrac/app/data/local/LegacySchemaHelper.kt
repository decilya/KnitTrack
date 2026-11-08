package com.knittrac.app.data.local

import android.database.sqlite.SQLiteDatabase

/**
 * Literal historical SQL для миграционных тестов (5→6…9→10).
 *
 * Зачем: тест должен стартовать с состояния «пользователь на vN», а не с
 * текущей `onCreate`-схемы. После смены схемы в v10 (удаление
 * `current_row_number`) onCreate даёт v10, и старые миграции падают на
 * несовместимом состоянии. Этот helper изолирует тесты от текущей схемы.
 *
 * SQL здесь — снимок схемы на vN. Не рефакторить под текущий код: цель —
 * защита от регрессий миграций. При появлении v11 создать
 * `createVersion(db, 11)`.
 */
object LegacySchemaHelper {

    /**
     * Создаёт схему на версии [version] (5..9) в пустой [db].
     *
     * @param db открытая SQLiteDatabase (raw, не Room).
     * @param version 5, 6, 7, 8 или 9.
     */
    fun createVersion(db: SQLiteDatabase, version: Int) {
        require(version in 5..9) { "LegacySchemaHelper supports v5..v9, got $version" }
        createBaseTables(db)
        if (version >= 6) createV6Triggers(db)
        if (version >= 7) createV7Symbols(db)
        if (version >= 8) createV8Triggers(db)
        if (version >= 9) createV9Triggers(db)
        db.version = version
    }

    private fun createBaseTables(db: SQLiteDatabase) {
        createPatternTables(db)
        createLegendTables(db)
        createProjectTables(db)
        createSessionTables(db)
    }

    private fun createPatternTables(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE patterns (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, technique TEXT NOT NULL, direction TEXT NOT NULL, " +
                "is_reversible INTEGER NOT NULL, background_image_uri TEXT, " +
                "image_offset_x REAL NOT NULL, image_offset_y REAL NOT NULL, " +
                "image_scale REAL NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE pattern_rows (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "pattern_id INTEGER NOT NULL, row_number INTEGER NOT NULL, " +
                "FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX index_pattern_rows_pattern_id ON pattern_rows (pattern_id)")
        db.execSQL("CREATE UNIQUE INDEX index_pattern_rows_pattern_id_row_number ON pattern_rows (pattern_id, row_number)")
    }

    private fun createLegendTables(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE symbols (code TEXT PRIMARY KEY NOT NULL, owner_pattern_id INTEGER, " +
                "display_name TEXT NOT NULL, text_glyph TEXT NOT NULL, category TEXT NOT NULL, " +
                "default_column_span INTEGER NOT NULL DEFAULT 1, " +
                "default_row_span INTEGER NOT NULL DEFAULT 1, " +
                "FOREIGN KEY(owner_pattern_id) REFERENCES patterns(id) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX index_symbols_owner_pattern_id ON symbols (owner_pattern_id)")
        db.execSQL(
            "CREATE TABLE pattern_operations (pattern_row_id INTEGER NOT NULL, " +
                "column_start INTEGER NOT NULL, symbol_code TEXT NOT NULL, " +
                "back_symbol_code TEXT, color_hex TEXT, " +
                "column_span_override INTEGER, row_span_override INTEGER, repeat_code TEXT, " +
                "PRIMARY KEY(pattern_row_id, column_start), " +
                "FOREIGN KEY(pattern_row_id) REFERENCES pattern_rows(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(symbol_code) REFERENCES symbols(code) ON DELETE RESTRICT, " +
                "FOREIGN KEY(back_symbol_code) REFERENCES symbols(code) ON DELETE RESTRICT)"
        )
        db.execSQL("CREATE INDEX index_pattern_operations_symbol_code ON pattern_operations (symbol_code)")
        db.execSQL("CREATE INDEX index_pattern_operations_back_symbol_code ON pattern_operations (back_symbol_code)")
        db.execSQL("CREATE INDEX index_pattern_operations_repeat_code ON pattern_operations (repeat_code)")
        db.execSQL(
            "CREATE TABLE pattern_repeats (pattern_id INTEGER NOT NULL, code TEXT NOT NULL, " +
                "stitch_count INTEGER NOT NULL, row_count INTEGER NOT NULL, " +
                "PRIMARY KEY(pattern_id, code), " +
                "FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE CASCADE)"
        )
    }

    private fun createProjectTables(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE projects (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL CHECK(length(trim(name)) > 0 AND length(name) <= 100), " +
                "category TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, " +
                "total_time_seconds INTEGER NOT NULL CHECK(total_time_seconds >= 0), " +
                "total_rows INTEGER NOT NULL DEFAULT 0 CHECK(total_rows >= 0), " +
                "target_row_count INTEGER, pattern_id INTEGER, " +
                "current_row_number INTEGER NOT NULL DEFAULT 0, " +
                "current_pattern_row_id INTEGER, " +
                "current_stitch INTEGER NOT NULL DEFAULT 0, sync_status TEXT NOT NULL, " +
                "FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE SET NULL, " +
                "FOREIGN KEY(current_pattern_row_id) REFERENCES pattern_rows(id) ON DELETE SET NULL)"
        )
        db.execSQL("CREATE INDEX index_projects_pattern_id ON projects (pattern_id)")
        db.execSQL("CREATE INDEX index_projects_current_pattern_row_id ON projects (current_pattern_row_id)")
        db.execSQL(
            "CREATE TABLE project_progress (project_id INTEGER NOT NULL, " +
                "pattern_row_id INTEGER NOT NULL, PRIMARY KEY(project_id, pattern_row_id), " +
                "FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(pattern_row_id) REFERENCES pattern_rows(id) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX index_project_progress_pattern_row_id ON project_progress (pattern_row_id)")
    }

    private fun createSessionTables(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "project_id INTEGER NOT NULL, start_time INTEGER NOT NULL, end_time INTEGER NOT NULL, " +
                "duration_seconds INTEGER NOT NULL CHECK(duration_seconds >= 0), " +
                "row_count INTEGER NOT NULL, updated_at INTEGER NOT NULL, sync_status TEXT NOT NULL, " +
                "FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX index_sessions_project_id ON sessions (project_id)")
        db.execSQL("CREATE INDEX index_sessions_start_time ON sessions (start_time)")
    }

    private fun createV6Triggers(db: SQLiteDatabase) {
        createV6InsertTrigger(db)
        createV6UpdateTrigger(db)
    }

    private fun createV6InsertTrigger(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TRIGGER trg_progress_check_insert BEFORE INSERT ON projects BEGIN " +
                "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
                "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_rows WHERE id = NEW.current_pattern_row_id " +
                "AND pattern_id IS NEW.pattern_id); " +
                "SELECT RAISE(ABORT, 'Номер ряда не соответствует денормализации') " +
                "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_rows WHERE id = NEW.current_pattern_row_id " +
                "AND row_number = NEW.current_row_number); " +
                "SELECT RAISE(ABORT, 'Прогресс без ряда должен иметь номер 0') " +
                "WHERE NEW.current_pattern_row_id IS NULL AND NEW.current_row_number != 0; " +
                "END"
        )
    }

    private fun createV6UpdateTrigger(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TRIGGER trg_progress_check_update BEFORE UPDATE OF " +
                "current_pattern_row_id, current_row_number, current_stitch, pattern_id " +
                "ON projects BEGIN " +
                "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
                "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_rows WHERE id = NEW.current_pattern_row_id " +
                "AND pattern_id IS NEW.pattern_id); " +
                "SELECT RAISE(ABORT, 'Номер ряда не соответствует денормализации') " +
                "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_rows WHERE id = NEW.current_pattern_row_id " +
                "AND row_number = NEW.current_row_number); " +
                "SELECT RAISE(ABORT, 'Прогресс без ряда должен иметь номер 0') " +
                "WHERE NEW.current_pattern_row_id IS NULL AND NEW.current_row_number != 0; " +
                "END"
        )
    }

    private fun createV7Symbols(db: SQLiteDatabase) {
        seedSymbol(db, "K", "Knit", "K", "BASIC")
        seedSymbol(db, "P", "Purl", "P", "BASIC")
        seedSymbol(db, "O", "Yarn over", "O", "BASIC")
        seedSymbol(db, "K2tog", "Knit 2 together", "K2tog", "DECREASE")
        seedSymbol(db, "SSK", "Slip slip knit", "\\", "DECREASE")
        seedSymbol(db, "SL", "Slip", "SL", "BASIC")
    }

    private fun createV8Triggers(db: SQLiteDatabase) {
        db.execSQL("DROP TRIGGER IF EXISTS trg_progress_check_insert")
        db.execSQL("DROP TRIGGER IF EXISTS trg_progress_check_update")
        createV8InsertTrigger(db)
        createV8UpdateTrigger(db)
    }

    private fun createV8InsertTrigger(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TRIGGER trg_progress_check_insert BEFORE INSERT ON projects BEGIN " +
                "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
                "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_rows WHERE id = NEW.current_pattern_row_id " +
                "AND pattern_id IS NEW.pattern_id); " +
                "SELECT RAISE(ABORT, 'Номер ряда не соответствует денормализации') " +
                "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_rows WHERE id = NEW.current_pattern_row_id " +
                "AND row_number = NEW.current_row_number); " +
                "SELECT RAISE(ABORT, 'Прогресс без ряда должен иметь номер 0') " +
                "WHERE NEW.current_pattern_row_id IS NULL AND NEW.current_row_number != 0; " +
                "SELECT RAISE(ABORT, 'Петли без ряда невозможны') " +
                "WHERE NEW.current_stitch > 0 AND NEW.current_pattern_row_id IS NULL; " +
                "END"
        )
    }

    private fun createV8UpdateTrigger(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TRIGGER trg_progress_check_update BEFORE UPDATE OF " +
                "current_pattern_row_id, current_row_number, current_stitch, pattern_id " +
                "ON projects BEGIN " +
                "SELECT RAISE(ABORT, 'Ряд не принадлежит схеме проекта') " +
                "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_rows WHERE id = NEW.current_pattern_row_id " +
                "AND pattern_id IS NEW.pattern_id); " +
                "SELECT RAISE(ABORT, 'Номер ряда не соответствует денормализации') " +
                "WHERE NEW.current_pattern_row_id IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_rows WHERE id = NEW.current_pattern_row_id " +
                "AND row_number = NEW.current_row_number); " +
                "SELECT RAISE(ABORT, 'Прогресс без ряда должен иметь номер 0') " +
                "WHERE NEW.current_pattern_row_id IS NULL AND NEW.current_row_number != 0; " +
                "SELECT RAISE(ABORT, 'Петли без ряда невозможны') " +
                "WHERE NEW.current_stitch > 0 AND NEW.current_pattern_row_id IS NULL; " +
                "END"
        )
    }

    private fun createV9Triggers(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TRIGGER trg_repeat_code_check_insert BEFORE INSERT ON pattern_operations BEGIN " +
                "SELECT RAISE(ABORT, 'Раппорт не принадлежит схеме') " +
                "WHERE NEW.repeat_code IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_repeats pr " +
                "INNER JOIN pattern_rows r ON r.pattern_id = pr.pattern_id " +
                "WHERE r.id = NEW.pattern_row_id AND pr.code = NEW.repeat_code); " +
                "END"
        )
        db.execSQL(
            "CREATE TRIGGER trg_repeat_code_check_update BEFORE UPDATE OF repeat_code " +
                "ON pattern_operations BEGIN " +
                "SELECT RAISE(ABORT, 'Раппорт не принадлежит схеме') " +
                "WHERE NEW.repeat_code IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_repeats pr " +
                "INNER JOIN pattern_rows r ON r.pattern_id = pr.pattern_id " +
                "WHERE r.id = NEW.pattern_row_id AND pr.code = NEW.repeat_code); " +
                "END"
        )
    }

    private fun seedSymbol(db: SQLiteDatabase, code: String, name: String, glyph: String, category: String) {
        db.execSQL(
            "INSERT OR IGNORE INTO symbols (code, owner_pattern_id, display_name, " +
                "text_glyph, category, default_column_span, default_row_span) " +
                "VALUES ('$code', NULL, '$name', '$glyph', '$category', 1, 1)"
        )
    }
}
