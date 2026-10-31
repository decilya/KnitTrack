package com.knittrac.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Тесты миграции 5→6 (7c-2): триггер `trg_progress_check` + санитизация
 * legacy-данных. Семантика инвариантов — docs/architecture/rows-and-patterns.md §6.
 *
 * Стратегия:
 * 1. Создаём БД v6 через Room (onCreate — все таблицы v6, триггер уже стоит).
 * 2. Raw: DROP TRIGGER, seed (валидные/невалидные данные), PRAGMA user_version = 5
 *    — симулируем v5 (v6 минус триггер).
 * 3. Room с MIGRATION_5_6 — триггер создаётся заново, санитизация запускается.
 * 4. Проверяем итоговое состояние raw-SELECT'ом.
 *
 * Почему не MigrationTestHelper: не работает в Robolectric (см. Migration4To5Test).
 */
@RunWith(RobolectricTestRunner::class)
class Migration5To6Test {

    private lateinit var context: Context
    private val dbName = "migration-5-6-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    // ============ helpers ============

    /** Создаёт БД v6 через Room onCreate (все таблицы, триггер). Миграции 1→5
     *  не запускаются — только onCreate сгенерированного _Impl. */
    private fun createV6AndClose() {
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .build()
        room.openHelper.writableDatabase.close()
        room.close()
    }

    /** Симулирует v5: drop trigger + seed + PRAGMA user_version = 5. */
    private fun seedV5(seed: (SQLiteDatabase) -> Unit) {
        createV6AndClose()
        val raw = SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        raw.execSQL("DROP TRIGGER IF EXISTS trg_progress_check")
        raw.execSQL("PRAGMA foreign_keys = OFF")
        seed(raw)
        raw.execSQL("PRAGMA user_version = 5")
        raw.close()
    }

    /** Открывает через Room с MIGRATION_5_6 — триггер создаётся, санитизация. */
    private fun runMigration(): AppDatabase {
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .build()
        room.openHelper.writableDatabase  // forces migration
        return room
    }

    private fun seedPatternAndRow(
        db: SQLiteDatabase,
        patternId: Long,
        rowId: Long,
        rowNumber: Int
    ) {
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

    private fun seedProject(
        db: SQLiteDatabase,
        projectId: Long,
        patternId: Long?,
        currentPatternRowId: Long?,
        currentRowNumber: Int
    ) {
        val pid = patternId?.toString() ?: "NULL"
        val rid = currentPatternRowId?.toString() ?: "NULL"
        db.execSQL(
            "INSERT INTO projects (id, name, category, created_at, updated_at, " +
                "total_time_seconds, sync_status, pattern_id, current_row_number, " +
                "current_pattern_row_id) VALUES " +
                "($projectId, 'Proj$projectId', 'KNITTING', 0, 0, 0, 'PENDING', " +
                "$pid, $currentRowNumber, $rid)"
        )
    }

    private fun readProjectProgress(room: AppDatabase, projectId: Long): Pair<Long?, Int> {
        return room.openHelper.readableDatabase.query(
            "SELECT current_pattern_row_id, current_row_number FROM projects WHERE id = $projectId"
        ).use { c ->
            assertTrue("project $projectId not found", c.moveToFirst())
            val rowId: Long? = if (c.isNull(0)) null else c.getLong(0)
            rowId to c.getInt(1)
        }
    }

    /** Оба триггера 7c-2: insert + update. */
    private fun triggerCount(room: AppDatabase): Int {
        return room.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM sqlite_master " +
                "WHERE type='trigger' AND name LIKE 'trg_progress_check%'"
        ).use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
    }

    // ============ tests ============

    /** Валидный progress (row ∈ pattern, number = row_number) — не трогается. */
    @Test
    fun migrate5To6_preservesValidProgress() {
        seedV5 { db ->
            seedPatternAndRow(db, patternId = 1, rowId = 10, rowNumber = 1)
            seedProject(db, projectId = 1, patternId = 1, currentPatternRowId = 10, currentRowNumber = 1)
        }
        val room = runMigration()
        val (rowId, rowNum) = readProjectProgress(room, 1)
        assertEquals(10L, rowId)
        assertEquals(1, rowNum)
        room.close()
    }

    /** A: row есть, но pattern_id = NULL → row и number обнуляются. */
    @Test
    fun migrate5To6_sanitizesRowNotInPattern() {
        seedV5 { db ->
            seedPatternAndRow(db, patternId = 1, rowId = 10, rowNumber = 1)
            seedProject(db, projectId = 1, patternId = null, currentPatternRowId = 10, currentRowNumber = 1)
        }
        val room = runMigration()
        val (rowId, rowNum) = readProjectProgress(room, 1)
        assertNull(rowId)
        assertEquals(0, rowNum)
        room.close()
    }

    /** A: row есть, но из другой схемы → обнуляются. */
    @Test
    fun migrate5To6_sanitizesRowFromOtherPattern() {
        seedV5 { db ->
            seedPatternAndRow(db, patternId = 1, rowId = 10, rowNumber = 1)
            seedPatternAndRow(db, patternId = 2, rowId = 20, rowNumber = 1)
            seedProject(db, projectId = 1, patternId = 1, currentPatternRowId = 20, currentRowNumber = 1)
        }
        val room = runMigration()
        val (rowId, rowNum) = readProjectProgress(room, 1)
        assertNull(rowId)
        assertEquals(0, rowNum)
        room.close()
    }

    /** B-fix: row валиден, number рассинхронизирован → number исправлен, row сохранён.
     *  Критический регресс: составной UPDATE терял бы row. */
    @Test
    fun migrate5To6_fixesRowNumber_keepsRow() {
        seedV5 { db ->
            seedPatternAndRow(db, patternId = 1, rowId = 10, rowNumber = 5)
            seedProject(db, projectId = 1, patternId = 1, currentPatternRowId = 10, currentRowNumber = 99)
        }
        val room = runMigration()
        val (rowId, rowNum) = readProjectProgress(room, 1)
        assertEquals("row должен быть сохранён (B-fix, не потерян)", 10L, rowId)
        assertEquals("number исправлен по row_number", 5, rowNum)
        room.close()
    }

    /** C: row IS NULL, number != 0 → number = 0. */
    @Test
    fun migrate5To6_sanitizesNumberWhenRowNull() {
        seedV5 { db ->
            seedProject(db, projectId = 1, patternId = null, currentPatternRowId = null, currentRowNumber = 7)
        }
        val room = runMigration()
        val (rowId, rowNum) = readProjectProgress(room, 1)
        assertNull(rowId)
        assertEquals(0, rowNum)
        room.close()
    }

    /** Триггер создан после миграции (Room не экспортирует триггеры в 6.json —
     *  проверка через sqlite_master, а не через schema validation). */
    @Test
    fun migrate5To6_createsTrigger() {
        seedV5 { /* пустая БД */ }
        val room = runMigration()
        assertEquals("должно быть 2 триггера (insert + update)", 2, triggerCount(room))
        room.close()
    }

    /** A (runtime): UPDATE на row из другой схемы → SQLiteConstraintException.
     *  FK passes (row 20 существует в pattern_rows), Trigger A отбивает. */
    @Test
    fun trigger_rejectsRowNotInPattern() {
        seedV5 { db ->
            seedPatternAndRow(db, patternId = 1, rowId = 10, rowNumber = 1)
            seedPatternAndRow(db, patternId = 2, rowId = 20, rowNumber = 1)
            seedProject(db, projectId = 1, patternId = 1, currentPatternRowId = 10, currentRowNumber = 1)
        }
        val room = runMigration()
        try {
            room.openHelper.writableDatabase.execSQL(
                "UPDATE projects SET current_pattern_row_id = 20 WHERE id = 1"
            )
            throw AssertionError("Ожидалось SQLiteConstraintException от Trigger A")
        } catch (expected: SQLiteConstraintException) {
            // ok
        }
        room.close()
    }

    /** B (runtime): UPDATE на number, не совпадающий с row_number → SQLiteConstraintException. */
    @Test
    fun trigger_rejectsNumberMismatch() {
        seedV5 { db ->
            seedPatternAndRow(db, patternId = 1, rowId = 10, rowNumber = 5)
            seedProject(db, projectId = 1, patternId = 1, currentPatternRowId = 10, currentRowNumber = 5)
        }
        val room = runMigration()
        try {
            room.openHelper.writableDatabase.execSQL(
                "UPDATE projects SET current_row_number = 99 WHERE id = 1"
            )
            throw AssertionError("Ожидалось SQLiteConstraintException от Trigger B")
        } catch (expected: SQLiteConstraintException) {
            // ok
        }
        room.close()
    }
}
