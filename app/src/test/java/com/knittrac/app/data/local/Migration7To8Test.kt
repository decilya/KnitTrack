package com.knittrac.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Тесты миграции 7→8 (7f-D-5): добавление D-инварианта в триггеры.
 *
 * Стратегия:
 * 1. Создаём БД v8 (Room onCreate, триггеры с правилом D).
 * 2. Raw: DROP TRIGGER, seed, PRAGMA user_version = 7 (симулируем v7).
 * 3. Room с MIGRATION_7_8 — санитизация + пересоздание триггеров.
 * 4. Проверяем состояние.
 */
@RunWith(RobolectricTestRunner::class)
class Migration7To8Test {
    private lateinit var context: Context
    private val dbName = "migration-7-8-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createV7AndClose() {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        LegacySchemaHelper.createVersion(db, 7)
        db.close()
    }

    private fun seedV7(seed: (SQLiteDatabase) -> Unit) {
        createV7AndClose()
        val raw = SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        raw.execSQL("DROP TRIGGER IF EXISTS trg_progress_check_insert")
        raw.execSQL("DROP TRIGGER IF EXISTS trg_progress_check_update")
        raw.execSQL("PRAGMA foreign_keys = OFF")
        seed(raw)
        raw.execSQL("PRAGMA user_version = 7")
        raw.close()
    }

    private fun runMigration(): AppDatabase {
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabaseMigrations.MIGRATION_7_8, AppDatabaseMigrations.MIGRATION_8_9, AppDatabaseMigrations.MIGRATION_9_10)
            .build()
        room.openHelper.writableDatabase
        return room
    }

    private fun seedPatternAndRow(db: SQLiteDatabase, patternId: Long, rowId: Long, rowNumber: Int) {
        db.execSQL(
            "INSERT INTO patterns (id, name, technique, direction, is_reversible, " +
                "image_offset_x, image_offset_y, image_scale) VALUES " +
                "($patternId, 'P$patternId', 'KNITTING', 'LTR', 0, 0.0, 0.0, 1.0)"
        )
        db.execSQL(
            "INSERT INTO pattern_rows (id, pattern_id, row_number) VALUES " +
                "($rowId, $patternId, $rowNumber)"
        )
    }

    @Suppress("LongParameterList")
    private fun seedProject(
        db: SQLiteDatabase,
        projectId: Long,
        patternId: Long?,
        currentPatternRowId: Long?,
        currentRowNumber: Int,
        currentStitch: Int
    ) {
        val pid = patternId?.toString() ?: "NULL"
        val rid = currentPatternRowId?.toString() ?: "NULL"
        db.execSQL(
            "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                "total_time_seconds, sync_status, pattern_id, current_row_number, " +
                "current_pattern_row_id, current_stitch) VALUES " +
                "($projectId, 'Proj$projectId', 'KNITTING', 0, 0, 0, 'PENDING', " +
                "$pid, $currentRowNumber, $rid, $currentStitch)"
        )
    }

    private fun readProjectStitch(room: AppDatabase, projectId: Long): Int {
        return room.openHelper.readableDatabase.query(
            "SELECT current_stitch FROM projects WHERE id = $projectId"
        ).use { c ->
            assertTrue(c.moveToFirst())
            c.getInt(0)
        }
    }

    @Test
    fun migrate7To8_preservesValidStitch() {
        seedV7 { db ->
            seedPatternAndRow(db, patternId = 1, rowId = 10, rowNumber = 1)
            seedProject(db, projectId = 1, patternId = 1, currentPatternRowId = 10, currentRowNumber = 1, currentStitch = 5)
        }
        val room = runMigration()
        assertEquals(5, readProjectStitch(room, 1))
        room.close()
    }

    @Test
    fun migrate7To8_sanitizesStitchWithoutRow() {
        seedV7 { db ->
            seedProject(db, projectId = 1, patternId = null, currentPatternRowId = null, currentRowNumber = 0, currentStitch = 5)
        }
        val room = runMigration()
        assertEquals(0, readProjectStitch(room, 1))
        room.close()
    }

    @Test
    fun trigger_rejectsStitchWithoutRowOnUpdate() {
        seedV7 { db ->
            seedProject(db, projectId = 1, patternId = null, currentPatternRowId = null, currentRowNumber = 0, currentStitch = 0)
        }
        val room = runMigration()
        try {
            room.openHelper.writableDatabase.execSQL(
                "UPDATE projects SET current_stitch = 5 WHERE id = 1"
            )
            throw AssertionError("Ожидалось SQLiteConstraintException от Trigger D")
        } catch (expected: SQLiteConstraintException) {
            // ok
        }
        room.close()
    }

    @Test
    fun freshInstall_createsTriggerWithRuleD() {
        context.deleteDatabase(dbName)
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addCallback(DatabaseCallback)
            .build()
        room.openHelper.writableDatabase

        try {
            room.openHelper.writableDatabase.execSQL(
                "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                    "total_time_seconds, sync_status, pattern_id, " +
                    "current_pattern_row_id, current_stitch) VALUES " +
                    "(1, 'P', 'KNITTING', 0, 0, 0, 'PENDING', NULL, NULL, 5)"
            )
            throw AssertionError("Ожидалось SQLiteConstraintException на INSERT от Trigger D")
        } catch (expected: SQLiteConstraintException) {
            // ok
        }
        room.close()
    }
}
