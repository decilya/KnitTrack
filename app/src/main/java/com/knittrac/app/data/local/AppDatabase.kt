package com.knittrac.app.data.local

import androidx.room.Database
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
 * 2. Текущая версия схемы: 10 (см. `version = 10`).
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
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun patternDao(): PatternDao
    abstract fun symbolDao(): SymbolDao
    abstract fun patternRepeatDao(): PatternRepeatDao

    companion object {
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
        private const val CREATE_PROJECTS_WITH_CHECK =
            "CREATE TABLE projects_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL CHECK(length(trim(name)) > 0 AND length(name) <= 100), category TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, total_time_seconds INTEGER NOT NULL CHECK(total_time_seconds >= 0), total_rows INTEGER NOT NULL DEFAULT 0 CHECK(total_rows >= 0), target_row_count INTEGER, pattern_id INTEGER, current_row_number INTEGER NOT NULL DEFAULT 0, current_pattern_row_id INTEGER, current_stitch INTEGER NOT NULL DEFAULT 0, sync_status TEXT NOT NULL, FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE SET NULL, FOREIGN KEY(current_pattern_row_id) REFERENCES pattern_rows(id) ON DELETE SET NULL)"

        private const val CREATE_INDEX_PROJECTS_PATTERN_ID =
            "CREATE INDEX index_projects_pattern_id ON projects_new (pattern_id)"

        private const val CREATE_INDEX_PROJECTS_CURRENT_ROW =
            "CREATE INDEX index_projects_current_pattern_row_id ON projects_new (current_pattern_row_id)"

        private const val CREATE_SESSIONS_WITH_CHECK =
            "CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, project_id INTEGER NOT NULL, start_time INTEGER NOT NULL, end_time INTEGER NOT NULL, duration_seconds INTEGER NOT NULL CHECK(duration_seconds >= 0), row_count INTEGER NOT NULL, updated_at INTEGER NOT NULL, sync_status TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)"

        private const val CREATE_INDEX_SESSIONS_PROJECT_ID =
            "CREATE INDEX index_sessions_project_id ON sessions (project_id)"

        private const val CREATE_INDEX_SESSIONS_START_TIME =
            "CREATE INDEX index_sessions_start_time ON sessions (start_time)"

        private const val CREATE_PROJECT_PROGRESS =
            "CREATE TABLE project_progress (project_id INTEGER NOT NULL, pattern_row_id INTEGER NOT NULL, PRIMARY KEY(project_id, pattern_row_id), FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE, FOREIGN KEY(pattern_row_id) REFERENCES pattern_rows(id) ON DELETE CASCADE)"

        private const val CREATE_INDEX_PROJECT_PROGRESS_ROW =
            "CREATE INDEX index_project_progress_pattern_row_id ON project_progress (pattern_row_id)"


        /**
         * CREATE TABLE projects_new для v10: без current_row_number (3NF).
         *
         * currentRowNumber — derived от current_pattern_row_id через
         * LEFT JOIN pattern_rows.row_number. Дублирование в projects
         * устранено (P2, 3NF).
         */
        private const val CREATE_PROJECTS_V10 =
            "CREATE TABLE projects_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL CHECK(length(trim(name)) > 0 AND length(name) <= 100), category TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, total_time_seconds INTEGER NOT NULL CHECK(total_time_seconds >= 0), total_rows INTEGER NOT NULL DEFAULT 0 CHECK(total_rows >= 0), target_row_count INTEGER, pattern_id INTEGER, current_pattern_row_id INTEGER, current_stitch INTEGER NOT NULL DEFAULT 0, sync_status TEXT NOT NULL, FOREIGN KEY(pattern_id) REFERENCES patterns(id) ON DELETE SET NULL, FOREIGN KEY(current_pattern_row_id) REFERENCES pattern_rows(id) ON DELETE SET NULL)"

        /**
         * v10: A (row ∈ pattern) + D (stitch > 0 → row != null).
         * B (current_row_number = row_number) и C (row IS NULL → number = 0)
         * удалены — колонки current_row_number больше нет, инвариант derived.
         */
        private const val CREATE_TRIGGER_PROGRESS_INSERT_V10 =
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

        private const val CREATE_TRIGGER_PROGRESS_UPDATE_V10 =
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

        /**
         * MIGRATION_9_10 (P2, 3NF): удаление current_row_number.
         *
         * Порядок (FK children от projects — sessions, project_progress):
         * 1. Бэкап children.
         * 2. DROP children.
         * 3. CREATE projects_new (без current_row_number) + индексы.
         * 4. COPY из старой projects.
         * 5. DROP old projects.
         * 6. RENAME projects_new -> projects.
         * 7. Recreate children + restore.
         * 8. Триггеры A+D (B и C удалены — current_row_number нет).
         *
         * PRAGMA foreign_keys = OFF внутри migrate() — no-op (Room-транзакция).
         * Безопасность обеспечивается порядком: children бэкапятся и удаляются
         * ДО DROP parent, восстанавливаются ПОСЛЕ RENAME.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Бэкап children (sessions, project_progress)
                db.execSQL("CREATE TABLE sessions_backup (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, project_id INTEGER NOT NULL, start_time INTEGER NOT NULL, end_time INTEGER NOT NULL, duration_seconds INTEGER NOT NULL, row_count INTEGER NOT NULL, updated_at INTEGER NOT NULL, sync_status TEXT NOT NULL)")
                db.execSQL("INSERT INTO sessions_backup SELECT id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status FROM sessions")

                db.execSQL("CREATE TABLE project_progress_backup (project_id INTEGER NOT NULL, pattern_row_id INTEGER NOT NULL, PRIMARY KEY(project_id, pattern_row_id))")
                db.execSQL("INSERT INTO project_progress_backup SELECT project_id, pattern_row_id FROM project_progress")

                // 2. DROP children
                db.execSQL("DROP TABLE sessions")
                db.execSQL("DROP TABLE project_progress")

                // 3. CREATE projects_new (без current_row_number).
                // DROP INDEX обязателен: v5+ projects уже имеет
                // index_projects_pattern_id / index_projects_current_pattern_row_id,
                // а CREATE INDEX с тем же именем в SQLite — глобальный конфликт.
                db.execSQL(CREATE_PROJECTS_V10)
                db.execSQL("DROP INDEX IF EXISTS index_projects_pattern_id")
                db.execSQL("DROP INDEX IF EXISTS index_projects_current_pattern_row_id")
                db.execSQL(CREATE_INDEX_PROJECTS_PATTERN_ID)
                db.execSQL(CREATE_INDEX_PROJECTS_CURRENT_ROW)

                // 4. COPY из старой projects (до DROP)
                db.execSQL("INSERT INTO projects_new (id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, pattern_id, current_pattern_row_id, current_stitch, sync_status) SELECT id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, pattern_id, current_pattern_row_id, current_stitch, sync_status FROM projects")

                // 5. DROP old projects
                db.execSQL("DROP TABLE projects")

                // 6. RENAME
                db.execSQL("ALTER TABLE projects_new RENAME TO projects")

                // 7. Recreate children + restore
                db.execSQL(CREATE_SESSIONS_WITH_CHECK)
                db.execSQL(CREATE_INDEX_SESSIONS_PROJECT_ID)
                db.execSQL(CREATE_INDEX_SESSIONS_START_TIME)
                db.execSQL("INSERT INTO sessions SELECT id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status FROM sessions_backup")
                db.execSQL("DROP TABLE sessions_backup")

                db.execSQL(CREATE_PROJECT_PROGRESS)
                db.execSQL(CREATE_INDEX_PROJECT_PROGRESS_ROW)
                db.execSQL("INSERT INTO project_progress SELECT project_id, pattern_row_id FROM project_progress_backup")
                db.execSQL("DROP TABLE project_progress_backup")

                // 8. Триггеры A+D (B и C удалены)
                db.execSQL(CREATE_TRIGGER_PROGRESS_INSERT_V10)
                db.execSQL(CREATE_TRIGGER_PROGRESS_UPDATE_V10)
            }
        }

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
                db.execSQL(CREATE_PROJECTS_WITH_CHECK)
                db.execSQL(CREATE_INDEX_PROJECTS_PATTERN_ID)
                db.execSQL(CREATE_INDEX_PROJECTS_CURRENT_ROW)

                // 4. Переносим данные
                db.execSQL("INSERT INTO projects_new (id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, sync_status) SELECT id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, sync_status FROM projects")

                // 5. Удаляем старые таблицы (child first)
                db.execSQL("DROP TABLE sessions")
                db.execSQL("DROP TABLE projects")

                // 6. Переименовываем projects_new -> projects
                db.execSQL("ALTER TABLE projects_new RENAME TO projects")

                // 7. Восстанавливаем sessions с FK на projects
                db.execSQL(CREATE_SESSIONS_WITH_CHECK)
                db.execSQL(CREATE_INDEX_SESSIONS_PROJECT_ID)
                db.execSQL(CREATE_INDEX_SESSIONS_START_TIME)
                db.execSQL("INSERT INTO sessions SELECT id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status FROM sessions_backup")
                db.execSQL("DROP TABLE sessions_backup")

                // 8. Создаем project_progress (после переименования projects)
                db.execSQL(CREATE_PROJECT_PROGRESS)
                db.execSQL(CREATE_INDEX_PROJECT_PROGRESS_ROW)

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

        /**
         * Миграция с версии 5 на версию 6 (7c-2).
         *
         * Добавляет триггеры `trg_progress_check_insert` / `trg_progress_check_update`
         * (SQLite не поддерживает `INSERT OR UPDATE` в одном CREATE TRIGGER)
         * для защиты инвариантов
         * прогресса на уровне БД (см. docs/architecture/rows-and-patterns.md):
         * - A: current_pattern_row_id принадлежит схеме проекта (pattern_id);
         * - B: current_row_number = row_number денормализованного ряда;
         * - C: current_pattern_row_id IS NULL → current_row_number = 0.
         *
         * Порядок обязателен: сначала санитизация существующих данных
         * (иначе триггер отклонит UPDATE), затем CREATE TRIGGER.
         *
         * D-инвариант (stitch > 0 → row IS NOT NULL) — P3, включается
         * вместе с per-stitch tracking (7f-B). Сейчас current_stitch в БД
         * всегда 0 (RowMarkService.updateProgress → StitchCount.ZERO).
         */
        private const val CREATE_TRIGGER_PROGRESS_INSERT =
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

        private const val CREATE_TRIGGER_PROGRESS_INSERT_V8 =
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

        private const val CREATE_TRIGGER_PROGRESS_UPDATE_V8 =
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

        private const val CREATE_TRIGGER_PROGRESS_UPDATE =
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

        /**
         * Правило валидации repeat_code в pattern_operations (P2 #19, 05.11.2026).
         *
         * SQLite не может сделать FOREIGN KEY(repeat_code) -> pattern_repeats(code),
         * потому что PK pattern_repeats — composite (pattern_id, code), а code
         * глобально не уникален (два паттерна могут иметь повтор "A").
         *
         * Валидация через 3-way JOIN: pattern_operations.pattern_row_id ->
         * pattern_rows.pattern_id -> pattern_repeats(pattern_id, code).
         *
         * DELETE из pattern_repeats не покрыт: путей удаления repeat в app нет
         * (deleteRepeat отсутствует). При появлении DeleteRepeatService —
         * добавить третий триггер BEFORE DELETE ON pattern_repeats.
         */
        private const val CREATE_TRIGGER_REPEAT_INSERT_V9 =
            "CREATE TRIGGER trg_repeat_code_check_insert " +
                "BEFORE INSERT ON pattern_operations BEGIN " +
                "SELECT RAISE(ABORT, 'Раппорт не принадлежит схеме') " +
                "WHERE NEW.repeat_code IS NOT NULL AND NOT EXISTS (" +
                "SELECT 1 FROM pattern_repeats pr " +
                "INNER JOIN pattern_rows r ON r.pattern_id = pr.pattern_id " +
                "WHERE r.id = NEW.pattern_row_id " +
                "AND pr.code = NEW.repeat_code); " +
                "END"

        /**
         * P3 WHEN-оптимизация (07.11.2026): тело триггера пропускается,
         * если NEW.repeat_code совпадает с OLD (включая NULL → NULL).
         * `PatternDao.updateOperation` всегда пишет `repeat_code` в SET,
         * поэтому без WHEN 3-way JOIN выполнялся бы при каждом UPDATE
         * (смена цвета/символа без изменения repeat_code).
         *
         * `IS NOT` (не `!=`) — NULL-safe: `NULL IS NOT NULL` = false,
         * `NULL IS NOT 'A'` = true. С `!=` WHEN дал бы NULL и тело
         * выполнялось бы при NULL → NULL.
         *
         * Изменение касается fresh installs (onCreate) и новых v8→v9
         * миграций. Уже-мигрированные v9 сохраняют старый триггер
         * до v10 (P4, не актуально для pre-release).
         */
        private const val CREATE_TRIGGER_REPEAT_UPDATE_V9 =
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

        /**
         * Fresh install: CHECK на projects/sessions и триггеры прогресса.
         *
         * Room `onCreate` создаёт таблицы без CHECK. Миграции 2→3 и 4→5
         * добавляют CHECK только при upgrade. Callback пересоздаёт пустые
         * `projects` и `sessions` тем же SQL.
         *
         * `project_progress` не дропается: на fresh install она пустая, FK
         * не нарушается. `PRAGMA foreign_keys = OFF` на время DROP/CREATE,
         * `ON` — до триггеров, они пишут в `project_progress`.
         */
        internal val DATABASE_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("PRAGMA foreign_keys = OFF")
                db.execSQL("DROP TABLE sessions")
                db.execSQL("DROP TABLE projects")
                db.execSQL(CREATE_PROJECTS_V10)
                db.execSQL(CREATE_INDEX_PROJECTS_PATTERN_ID)
                db.execSQL(CREATE_INDEX_PROJECTS_CURRENT_ROW)
                db.execSQL("ALTER TABLE projects_new RENAME TO projects")
                db.execSQL(CREATE_SESSIONS_WITH_CHECK)
                db.execSQL(CREATE_INDEX_SESSIONS_PROJECT_ID)
                db.execSQL(CREATE_INDEX_SESSIONS_START_TIME)
                db.execSQL("PRAGMA foreign_keys = ON")
                db.execSQL(CREATE_TRIGGER_PROGRESS_INSERT_V10)
                db.execSQL(CREATE_TRIGGER_PROGRESS_UPDATE_V10)
                db.execSQL(CREATE_TRIGGER_REPEAT_INSERT_V9)
                db.execSQL(CREATE_TRIGGER_REPEAT_UPDATE_V9)
                db.execSQL(DEFAULT_SYMBOLS_INSERT)
            }
        }

        /**
         * Шесть системных символов. `owner_pattern_id` пустой — они в легенде
         * любой схемы. `INSERT OR IGNORE` не дублирует код при повторном прогоне.
         * Глиф SSK — один `\`: в этой строке слэш литеральный, SQLite его не экранирует.
         */
        private const val DEFAULT_SYMBOLS_INSERT = """
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
                // 1. Санитизация A+B: row невалиден → обнулить row и number
                db.execSQL(
                    "UPDATE projects SET current_pattern_row_id = NULL, " +
                        "current_row_number = 0 " +
                        "WHERE current_pattern_row_id IS NOT NULL AND (" +
                        "pattern_id IS NULL OR NOT EXISTS (" +
                        "SELECT 1 FROM pattern_rows " +
                        "WHERE id = current_pattern_row_id " +
                        "AND pattern_id = projects.pattern_id))"
                )

                // 2. Санитизация B-fix: row валиден, number рассинхронизирован → починить
                db.execSQL(
                    "UPDATE projects SET current_row_number = (" +
                        "SELECT row_number FROM pattern_rows " +
                        "WHERE id = current_pattern_row_id) " +
                        "WHERE current_pattern_row_id IS NOT NULL " +
                        "AND current_row_number != (" +
                        "SELECT row_number FROM pattern_rows " +
                        "WHERE id = current_pattern_row_id)"
                )

                // 3. Санитизация C: row NULL, number != 0 → 0
                db.execSQL(
                    "UPDATE projects SET current_row_number = 0 " +
                        "WHERE current_pattern_row_id IS NULL " +
                        "AND current_row_number != 0"
                )

                // 4. CREATE TRIGGER (SQLite: одно событие на триггер).
                // INSERT: прогресс должен быть NULL/0/0.
                db.execSQL(CREATE_TRIGGER_PROGRESS_INSERT)

                // UPDATE: только progress-колонки (OF фильтрует события).
                db.execSQL(CREATE_TRIGGER_PROGRESS_UPDATE)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Санитизация D: stitch > 0 при row IS NULL -> 0
                db.execSQL(
                    "UPDATE projects SET current_stitch = 0 " +
                    "WHERE current_pattern_row_id IS NULL AND current_stitch > 0"
                )

                // 2. DROP старых триггеров (без правила D)
                db.execSQL("DROP TRIGGER IF EXISTS trg_progress_check_insert")
                db.execSQL("DROP TRIGGER IF EXISTS trg_progress_check_update")

                // 3. CREATE новых триггеров (с правилом D)
                db.execSQL(CREATE_TRIGGER_PROGRESS_INSERT_V8)
                db.execSQL(CREATE_TRIGGER_PROGRESS_UPDATE_V8)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(DEFAULT_SYMBOLS_INSERT)
            }
        }

        /**
         * Миграция 8→9 (P2 #19, 05.11.2026): defense-in-depth для repeat_code.
         *
         * 1. Санитизация: orphan repeat_code -> NULL (до CREATE триггеров,
         *    иначе INSERT/UPDATE упадёт на ABORT при миграции).
         * 2. CREATE 2 триггеров (INSERT + UPDATE OF repeat_code).
         */
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
}
