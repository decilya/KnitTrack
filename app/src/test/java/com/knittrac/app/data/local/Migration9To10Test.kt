package com.knittrac.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Тесты миграции 9→10 (P2, 3NF): удаление `projects.current_row_number`.
 *
 * Стратегия:
 * 1. LegacySchemaHelper.createVersion(db, 9) — историческая схема v9 с колонкой.
 * 2. Seed проектов/sessions/progress на v9.
 * 3. Room с MIGRATION_9_10 — колонка удаляется, триггеры пересоздаются (A+D).
 * 4. Проверяем: колонки нет, данные сохранены, B/C-триггеров нет, A/D живы.
 */
@RunWith(RobolectricTestRunner::class)
class Migration9To10Test {

    private lateinit var context: Context
    private val dbName = "migration-9-10-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createLegacyV9(seed: (SQLiteDatabase) -> Unit = {}) {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        LegacySchemaHelper.createVersion(db, 9)
        seed(db)
        db.version = 9
        db.close()
    }

    private fun runMigration(): AppDatabase {
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabaseMigrations.MIGRATION_9_10)
            .build()
        room.openHelper.writableDatabase // forces migration
        return room
    }

    private fun seedPatternAndRow(db: SQLiteDatabase) {
        db.execSQL(
            "INSERT INTO patterns (id, name, technique, direction, is_reversible, " +
                "image_offset_x, image_offset_y, image_scale) VALUES " +
                "(1, 'P1', 'KNITTING', 'LTR', 0, 0.0, 0.0, 1.0)"
        )
        db.execSQL("INSERT INTO pattern_rows (id, pattern_id, row_number) VALUES (10, 1, 5)")
    }

    /**
     * Миграция 9→10 сохраняет projects и sessions без потерь.
     * current_pattern_row_id и current_stitch не теряются.
     */
    @Test
    fun migrate9To10_preservesProjectsAndSessions() {
        createLegacyV9 { db ->
            seedPatternAndRow(db)
            db.execSQL(
                "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                    "total_time_seconds, sync_status, pattern_id, current_row_number, " +
                    "current_pattern_row_id, current_stitch) VALUES " +
                    "(1, 'Свитер', 'KNITTING', 100, 200, 3600, 'PENDING', 1, 5, 10, 42)"
            )
            db.execSQL(
                "INSERT INTO sessions (id, project_id, start_time, end_time, " +
                    "duration_seconds, row_count, updated_at, sync_status) VALUES " +
                    "(1, 1, 100, 200, 100, 5, 200, 'PENDING')"
            )
        }
        val room = runMigration()
        room.openHelper.readableDatabase.query(
            "SELECT name, current_pattern_row_id, current_stitch FROM projects WHERE id = 1"
        ).use { c ->
            assertTrue("project lost", c.moveToFirst())
            assertEquals("Свитер", c.getString(0))
            assertEquals(10L, c.getLong(1))
            assertEquals(42, c.getInt(2))
        }
        room.openHelper.readableDatabase.query(
            "SELECT project_id, row_count FROM sessions WHERE id = 1"
        ).use { c ->
            assertTrue("session lost", c.moveToFirst())
            assertEquals(1L, c.getLong(0))
            assertEquals(5, c.getInt(1))
        }
        room.close()
    }

    /**
     * Колонка current_row_number удалена из projects.
     */
    @Test
    fun migrate9To10_dropsCurrentRowNumberColumn() {
        createLegacyV9 { db ->
            seedPatternAndRow(db)
            db.execSQL(
                "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                    "total_time_seconds, sync_status, pattern_id, current_row_number, " +
                    "current_pattern_row_id, current_stitch) VALUES " +
                    "(1, 'P', 'KNITTING', 0, 0, 0, 'PENDING', 1, 5, 10, 0)"
            )
        }
        val room = runMigration()
        room.openHelper.readableDatabase.query("PRAGMA table_info(projects)").use { c ->
            val cols = mutableSetOf<String>()
            while (c.moveToNext()) cols += c.getString(c.getColumnIndexOrThrow("name"))
            assertTrue("current_row_number должна быть удалена, а есть: $cols",
                "current_row_number" !in cols)
            assertTrue("current_stitch должна остаться", "current_stitch" in cols)
            assertTrue("current_pattern_row_id должна остаться", "current_pattern_row_id" in cols)
        }
        room.close()
    }

    /**
     * project_progress сохраняется (children бэкапятся и восстанавливаются).
     */
    @Test
    fun migrate9To10_preservesProjectProgress() {
        createLegacyV9 { db ->
            seedPatternAndRow(db)
            db.execSQL(
                "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                    "total_time_seconds, sync_status, pattern_id, current_row_number, " +
                    "current_pattern_row_id, current_stitch) VALUES " +
                    "(1, 'P', 'KNITTING', 0, 0, 0, 'PENDING', 1, 5, 10, 0)"
            )
            db.execSQL("INSERT INTO project_progress (project_id, pattern_row_id) VALUES (1, 10)")
        }
        val room = runMigration()
        room.openHelper.readableDatabase.query(
            "SELECT project_id, pattern_row_id FROM project_progress"
        ).use { c ->
            assertTrue("project_progress lost", c.moveToFirst())
            assertEquals(1L, c.getLong(0))
            assertEquals(10L, c.getLong(1))
        }
        room.close()
    }

    /**
     * После миграции существует только trg_progress_check_insert/update.
     */
    @Test
    fun migrate9To10_createsTriggersAAndD() {
        createLegacyV9()
        val room = runMigration()
        room.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='trigger' AND name LIKE 'trg_progress_check%'"
        ).use { c ->
            val names = mutableSetOf<String>()
            while (c.moveToNext()) names += c.getString(0)
            assertEquals(setOf("trg_progress_check_insert", "trg_progress_check_update"), names)
        }
        room.close()
    }

    /**
     * A-триггер: ряд не из схемы проекта → reject.
     */
    @Test
    fun migrate9To10_triggerARejectsForeignRow() {
        createLegacyV9 { db ->
            seedPatternAndRow(db)
            db.execSQL(
                "INSERT INTO patterns (id, name, technique, direction, is_reversible, " +
                    "image_offset_x, image_offset_y, image_scale) VALUES " +
                    "(2, 'P2', 'KNITTING', 'LTR', 0, 0.0, 0.0, 1.0)"
            )
        }
        val room = runMigration()
        assertThrows(SQLiteConstraintException::class.java) {
            room.openHelper.writableDatabase.execSQL(
                "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                    "total_time_seconds, sync_status, pattern_id, " +
                    "current_pattern_row_id, current_stitch) VALUES " +
                    "(99, 'Bad', 'KNITTING', 0, 0, 0, 'PENDING', 2, 10, 0)"
            )
        }
        room.close()
    }

    /**
     * D-триггер: stitch > 0 при row = NULL → reject.
     */
    @Test
    fun migrate9To10_triggerDRejectsStitchWithoutRow() {
        createLegacyV9()
        val room = runMigration()
        assertThrows(SQLiteConstraintException::class.java) {
            room.openHelper.writableDatabase.execSQL(
                "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                    "total_time_seconds, sync_status, pattern_id, " +
                    "current_pattern_row_id, current_stitch) VALUES " +
                    "(99, 'Bad', 'KNITTING', 0, 0, 0, 'PENDING', NULL, NULL, 5)"
            )
        }
        room.close()
    }

    /**
     * Derived чтение: LEFT JOIN даёт current_row_number из pattern_rows.
     */
    @Test
    fun migrate9To10_joinDerivesCurrentRowNumber() {
        createLegacyV9 { db ->
            seedPatternAndRow(db)
            db.execSQL(
                "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                    "total_time_seconds, sync_status, pattern_id, current_row_number, " +
                    "current_pattern_row_id, current_stitch) VALUES " +
                    "(1, 'P', 'KNITTING', 0, 0, 0, 'PENDING', 1, 5, 10, 0)"
            )
            db.execSQL(
                "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                    "total_time_seconds, sync_status, pattern_id, current_row_number, " +
                    "current_pattern_row_id, current_stitch) VALUES " +
                    "(2, 'NoPattern', 'KNITTING', 0, 0, 0, 'PENDING', NULL, 0, NULL, 0)"
            )
        }
        val room = runMigration()
        room.openHelper.readableDatabase.query(
            "SELECT p.id, COALESCE(pr.row_number, 0) AS n " +
                "FROM projects p LEFT JOIN pattern_rows pr ON pr.id = p.current_pattern_row_id " +
                "ORDER BY p.id"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1L, c.getLong(0))
            assertEquals(5, c.getInt(1))
            assertTrue(c.moveToNext())
            assertEquals(2L, c.getLong(0))
            assertEquals(0, c.getInt(1))
        }
        room.close()
    }
}
