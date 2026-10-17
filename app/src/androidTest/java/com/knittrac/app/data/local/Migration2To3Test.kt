package com.knittrac.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Инструментальные тесты миграции Room 2 -> 3.
 *
 * Покрывают три риска рукописного MIGRATION_2_3:
 * 1. SQL-синтаксис пересоздания таблиц с CHECK и FK — применяется ли без ошибок.
 * 2. Санитизация старых данных до наложения CHECK — заменяются ли
 *    пустые имена и отрицательные длительности на безопасные fallback.
 * 3. FK CASCADE на уровне БД — удаляются ли сессии при удалении проекта
 *    (важно: PRAGMA foreign_keys должен быть ON, иначе CASCADE не сработает).
 *
 * Требует эмулятор или устройство:
 *   .\gradlew.bat connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class Migration2To3Test {

    private val testDb = "migration-2-3-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    /**
     * Пустая БД версии 2 миграцией приводится к версии 3 без ошибок.
     * runMigrationsAndValidate валидирует итоговую схему против @Entity.
     */
    @Test
    fun migration2to3_appliesCleanly() {
        helper.createDatabase(testDb, 2).apply { close() }

        val db = helper.runMigrationsAndValidate(
            testDb,
            3,
            true,
            AppDatabase.MIGRATION_2_3,
        )
        db.close()
    }

    /**
     * Некорректные данные в v2 санитизируются до наложения CHECK —
     * миграция не падает на constraint.
     */
    @Test
    fun migration2to3_sanitizesOldData() {
        val v2 = helper.createDatabase(testDb, 2)
        v2.execSQL(
            "INSERT INTO projects (id, name, category, created_at, " +
                "updated_at, total_time_seconds, sync_status) " +
                "VALUES (1, '   ', 'KNITTING', 1000, 1000, -500, 'PENDING')"
        )
        v2.execSQL(
            "INSERT INTO sessions (id, project_id, start_time, end_time, " +
                "duration_seconds, row_count, updated_at, sync_status) " +
                "VALUES (1, 1, 1000, 2000, -100, 5, 2000, 'PENDING')"
        )
        v2.close()

        val v3 = helper.runMigrationsAndValidate(
            testDb,
            3,
            true,
            AppDatabase.MIGRATION_2_3,
        )

        v3.query("SELECT name FROM projects WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(
                "Пустое имя должно стать fallback",
                "Untitled",
                c.getString(0),
            )
        }

        v3.query("SELECT total_time_seconds FROM projects WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(
                "Отрицательное total_time_seconds должно стать 0",
                0L,
                c.getLong(0),
            )
        }

        v3.query("SELECT duration_seconds FROM sessions WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(
                "Отрицательная duration_seconds должна стать 0",
                0L,
                c.getLong(0),
            )
        }

        v3.close()
    }

    /**
     * FK CASCADE на уровне БД: удаление проекта удаляет его сессии.
     * Проверяет, что CASCADE реально активен, а не только декларирован.
     */
    @Test
    fun cascadeDelete_removesSessions() {
        val v2 = helper.createDatabase(testDb, 2)
        v2.execSQL(
            "INSERT INTO projects (id, name, category, created_at, " +
                "updated_at, total_time_seconds, sync_status) " +
                "VALUES (10, 'Проект', 'KNITTING', 1000, 1000, 100, 'PENDING')"
        )
        v2.execSQL(
            "INSERT INTO sessions (id, project_id, start_time, end_time, " +
                "duration_seconds, row_count, updated_at, sync_status) " +
                "VALUES (100, 10, 1000, 2000, 60, 5, 2000, 'PENDING'), " +
                "(101, 10, 3000, 4000, 60, 5, 4000, 'PENDING')"
        )
        v2.close()

        val v3 = helper.runMigrationsAndValidate(
            testDb,
            3,
            true,
            AppDatabase.MIGRATION_2_3,
        )

        v3.query("SELECT COUNT(*) FROM sessions WHERE project_id = 10").use { c ->
            c.moveToFirst()
            assertEquals("До удаления должно быть 2 сессии", 2L, c.getLong(0))
        }

        v3.execSQL("DELETE FROM projects WHERE id = 10")

        v3.query("SELECT COUNT(*) FROM sessions WHERE project_id = 10").use { c ->
            c.moveToFirst()
            assertEquals(
                "CASCADE должен удалить обе сессии при удалении проекта",
                0L,
                c.getLong(0),
            )
        }

        v3.close()
    }
}
