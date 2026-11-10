package com.knittrac.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Тесты миграции 8→9 (P2 #19): defense-in-depth для repeat_code.
 */
@RunWith(RobolectricTestRunner::class)
class Migration8To9Test {

    private lateinit var context: Context
    private val dbName = "migration-8-9-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createV8AndClose() {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        LegacySchemaHelper.createVersion(db, 8)
        db.close()
    }

    private fun seedV8(seed: (SQLiteDatabase) -> Unit) {
        createV8AndClose()
        val raw = SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        raw.execSQL("DROP TRIGGER IF EXISTS trg_repeat_code_check_insert")
        raw.execSQL("DROP TRIGGER IF EXISTS trg_repeat_code_check_update")
        raw.execSQL("PRAGMA foreign_keys = OFF")
        seed(raw)
        raw.execSQL("PRAGMA user_version = 8")
        raw.close()
    }

    private fun runMigration(): AppDatabase {
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabaseMigrations.MIGRATION_8_9, AppDatabaseMigrations.MIGRATION_9_10)
            .addCallback(DatabaseCallback)
            .build()
        room.openHelper.writableDatabase // forces migration
        return room
    }

    private fun seedBaseData(db: SQLiteDatabase) {
        // Символ 'K' уже вставлен автоматически через DEFAULT_SYMBOLS_INSERT в DATABASE_CALLBACK!
        db.execSQL("INSERT INTO patterns (id, name, technique, direction, is_reversible, image_offset_x, image_offset_y, image_scale) VALUES (1, 'P1', 'KNITTING', 'LTR', 0, 0.0, 0.0, 1.0)")
        db.execSQL("INSERT INTO pattern_rows (id, pattern_id, row_number) VALUES (10, 1, 1)")
        db.execSQL("INSERT INTO pattern_repeats (pattern_id, code, stitch_count, row_count) VALUES (1, 'A', 2, 1)")
    }

    /**
     * Проверяет, что миграция создает оба триггера.
     */
    @Test
    fun migrate8To9_createsTriggers() {
        // v8 (историческая схема) НЕ содержит trg_repeat_code_check_* —
        // они появляются только в MIGRATION_8_9. Проверяем это после runMigration.
        seedV8 { }
        val room = runMigration()
        val cursor = room.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='trigger' AND name LIKE 'trg_repeat_code_check%'"
        )
        val count = cursor.count
        cursor.close()
        room.close()
        assertEquals(2, count)
    }

    /**
     * P3 (07.11.2026): UPDATE-триггер содержит WHEN-оптимизацию —
     * тело пропускается, если repeat_code не изменился (включая NULL → NULL).
     * Проверяем через sqlite_master.sql, что WHEN присутствует в DDL.
     */
    @Test
    fun migrate8To9_updateTriggerHasWhenOptimization() {
        // DDL триггера с WHEN-оптимизацией создаётся только в MIGRATION_8_9.
        seedV8 { }
        val room = runMigration()
        val sql = room.openHelper.readableDatabase.query(
            "SELECT sql FROM sqlite_master WHERE type='trigger' " +
                "AND name='trg_repeat_code_check_update'"
        ).use {
            assertTrue("Триггер trg_repeat_code_check_update не найден после MIGRATION_8_9", it.moveToFirst())
            it.getString(0)
        }
        assertTrue("Trigger DDL должен содержать WHEN-оптимизацию, а содержит: $sql",
            sql.contains("WHEN OLD.repeat_code IS NOT NEW.repeat_code"))
        room.close()
    }

    /**
     * Проверяет, что санитизация обнуляет orphan repeat_code.
     */
    @Test
    fun migrate8To9_sanitizesOrphanRepeatCode() {
        seedV8 { db ->
            seedBaseData(db)
            db.execSQL("INSERT INTO pattern_operations (pattern_row_id, column_start, symbol_code, repeat_code) VALUES (10, 0, 'K', 'ORPHAN')")
        }

        val room = runMigration()
        val cursor = room.openHelper.readableDatabase.query("SELECT repeat_code FROM pattern_operations WHERE repeat_code IS NOT NULL")
        val hasData = cursor.moveToFirst()
        cursor.close()
        assertTrue(!hasData)
        room.close()
    }

    /**
     * Проверяет, что триггер запрещает INSERT с orphan repeat_code.
     */
    @Test
    fun trigger_rejectsOrphanRepeatCodeOnInsert() {
        seedV8 { db -> seedBaseData(db) }
        val room = runMigration()

        assertThrows(SQLiteConstraintException::class.java) {
            room.openHelper.writableDatabase.execSQL(
                "INSERT INTO pattern_operations (pattern_row_id, column_start, symbol_code, repeat_code) VALUES (10, 0, 'K', 'ORPHAN')"
            )
        }
        room.close()
    }

    /**
     * Проверяет, что триггер запрещает UPDATE на orphan repeat_code.
     */
    @Test
    fun trigger_rejectsOrphanRepeatCodeOnUpdate() {
        seedV8 { db ->
            seedBaseData(db)
            db.execSQL("INSERT INTO pattern_operations (pattern_row_id, column_start, symbol_code, repeat_code) VALUES (10, 0, 'K', 'A')")
        }
        val room = runMigration()

        assertThrows(SQLiteConstraintException::class.java) {
            room.openHelper.writableDatabase.execSQL(
                "UPDATE pattern_operations SET repeat_code = 'ORPHAN' WHERE column_start = 0"
            )
        }
        room.close()
    }

    /**
     * Проверяет, что валидный repeat_code вставляется успешно.
     */
    @Test
    fun insert_validRepeatCode_succeeds() {
        seedV8 { db -> seedBaseData(db) }
        val room = runMigration()
        val db = room.openHelper.writableDatabase

        db.execSQL("INSERT INTO pattern_operations (pattern_row_id, column_start, symbol_code, repeat_code) VALUES (10, 0, 'K', 'A')")

        val cursor = db.query("SELECT repeat_code FROM pattern_operations")
        val hasData = cursor.moveToFirst()
        val code = if (hasData) cursor.getString(0) else null
        cursor.close()
        assertTrue(hasData)
        assertEquals("A", code)
        room.close()
    }

    /**
     * Проверяет, что repeat_code = NULL вставляется успешно (триггер пропускает).
     */
    @Test
    fun insert_nullRepeatCode_succeeds() {
        seedV8 { db -> seedBaseData(db) }
        val room = runMigration()
        val db = room.openHelper.writableDatabase

        db.execSQL("INSERT INTO pattern_operations (pattern_row_id, column_start, symbol_code, repeat_code) VALUES (10, 0, 'K', NULL)")

        val cursor = db.query("SELECT repeat_code FROM pattern_operations")
        val hasData = cursor.moveToFirst()
        val code = if (hasData) cursor.getString(0) else null
        cursor.close()
        assertTrue(hasData)
        assertNull(code)
        room.close()
    }
}
