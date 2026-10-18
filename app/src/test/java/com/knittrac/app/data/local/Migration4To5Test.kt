package com.knittrac.app.data.local

import android.content.Context
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
 * Тест миграции БД с версии 4 на версию 5.
 *
 * Так как MigrationTestHelper не работает в Robolectric (проблемы с assets),
 * мы создаем файл БД v4 вручную и открываем его через Room с миграцией.
 */
@RunWith(RobolectricTestRunner::class)
class Migration4To5Test {

    private lateinit var context: Context
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate4To5_preservesSessionsAndProjects() {
        // 1. Создаем базу данных v4 вручную
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val v4Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        
        // Схема projects v4
        v4Db.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `total_time_seconds` INTEGER NOT NULL, `total_rows` INTEGER NOT NULL DEFAULT 0, `target_row_count` INTEGER, `sync_status` TEXT NOT NULL)")
        
        // Схема sessions v4
        v4Db.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `project_id` INTEGER NOT NULL, `start_time` INTEGER NOT NULL, `end_time` INTEGER NOT NULL, `duration_seconds` INTEGER NOT NULL, `row_count` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `sync_status` TEXT NOT NULL, FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        
        // Добавляем тестовые данные
        v4Db.execSQL("INSERT INTO projects (id, name, category, created_at, updated_at, total_time_seconds, total_rows, target_row_count, sync_status) VALUES (1, 'Свитер', 'KNITTING', 100, 200, 3600, 10, 100, 'PENDING')")
        v4Db.execSQL("INSERT INTO sessions (id, project_id, start_time, end_time, duration_seconds, row_count, updated_at, sync_status) VALUES (1, 1, 100, 200, 100, 5, 200, 'PENDING')")
        
        // КРИТИЧЕСКИ ВАЖНО: Устанавливаем версию БД, иначе Room будет думать, что она пустая (v0)
        v4Db.version = 4
        
        v4Db.close()

        // 2. Открываем базу через Room с миграцией v5
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .allowMainThreadQueries() // Разрешаем для теста
            .build()

        // 3. Проверяем, что проект сохранился и новые поля добавились
        db.query("SELECT * FROM projects WHERE id = 1", null).use { cursor ->
            assertTrue("Проект не найден", cursor.moveToFirst())
            assertEquals("Свитер", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(10, cursor.getInt(cursor.getColumnIndexOrThrow("total_rows")))
            assertEquals(100, cursor.getInt(cursor.getColumnIndexOrThrow("target_row_count")))
            
            // Новые поля
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("current_row_number")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("current_stitch")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("pattern_id")))
        }

        // 4. Проверяем, что сессия не потерялась
        db.query("SELECT * FROM sessions WHERE id = 1", null).use { cursor ->
            assertTrue("Сессия потерялась при миграции", cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("project_id")))
            assertEquals(5, cursor.getInt(cursor.getColumnIndexOrThrow("row_count")))
        }

        // 5. Проверяем, что новые таблицы созданы
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('patterns', 'pattern_rows', 'project_progress')", null).use { cursor ->
            assertEquals("Не все новые таблицы созданы", 3, cursor.count)
        }

        db.close()
    }
}
