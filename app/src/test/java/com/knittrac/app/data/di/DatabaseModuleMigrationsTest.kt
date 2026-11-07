package com.knittrac.app.data.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.knittrac.app.data.local.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P3 (AND) защита от забытых миграций (07.11.2026).
 *
 * Проверяет, что production-путь [DatabaseModule.provideAppDatabase]
 * содержит все миграции до текущей версии [AppDatabase].
 *
 * **Механизм:** создаём БД с `PRAGMA user_version = CURRENT - 1`,
 * открываем через [DatabaseModule.provideAppDatabase]. Если в
 * `addMigrations(...)` забыта `MIGRATION_{CURRENT-1}_{CURRENT}` — Room
 * бросит `IllegalStateException`, тест упадёт.
 *
 * **Что ловит:**
 * - Добавили `MIGRATION_9_10` в companion, но забыли в [DatabaseModule]
 *   (и bump version до 10) — тест упадёт.
 * - Удалили миграцию из `addMigrations` случайно — тест упадёт.
 *
 * **Что НЕ ловит** (для этого есть `Migration{N}To{N+1}Test`):
 * - Некорректный SQL внутри конкретной миграции.
 * - Триггеры/CHECK, которые не пересоздаются на upgrade.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseModuleMigrationsTest {

    companion object {
        /**
         * Текущая версия [AppDatabase]. При апгрейде `version = N` в
         * `@Database` — обновить эту константу.
         *
         * Намеренно hardcoded: reflection-путь для `@Database.version`
         * недоступен (аннотация с `RetentionPolicy.CLASS`). Если забыть
         * обновить константу — тест попробует миграцию от `N-1`, и если
         * её нет — упадёт. Это сигнал.
         */
        private const val CURRENT_DB_VERSION = 9
        private const val PREVIOUS_DB_VERSION = CURRENT_DB_VERSION - 1
        private const val DB_NAME = "knittrac_database"
    }

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    /**
     * Создаёт БД в текущей версии (fresh install -> onCreate), затем
     * понижает `user_version` до `CURRENT - 1`, эмулируя состояние
     * «пользователь на предыдущей версии».
     */
    private fun seedPreviousVersion() {
        val room = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addCallback(AppDatabase.DATABASE_CALLBACK)
            .build()
        room.openHelper.writableDatabase.close()
        room.close()

        val raw = SQLiteDatabase.openDatabase(
            context.getDatabasePath(DB_NAME).absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        raw.execSQL("PRAGMA user_version = $PREVIOUS_DB_VERSION")
        raw.close()
    }

    /**
     * Проверяет, что production-путь [DatabaseModule.provideAppDatabase]
     * успешно мигрирует БД с `CURRENT - 1` до `CURRENT` версии.
     *
     * Если `MIGRATION_{CURRENT-1}_{CURRENT}` отсутствует в
     * `addMigrations(...)` — Room бросит `IllegalStateException`
     * ("A migration from N to M was required but not found").
     */
    @Test
    fun provideAppDatabaseMigratesFromPreviousVersion() {
        seedPreviousVersion()

        val db = try {
            DatabaseModule.provideAppDatabase(context)
        } catch (e: IllegalStateException) {
            throw AssertionError(
                "DatabaseModule.provideAppDatabase не смог мигрировать БД " +
                    "с $PREVIOUS_DB_VERSION до $CURRENT_DB_VERSION. " +
                    "Проверьте, что MIGRATION_${PREVIOUS_DB_VERSION}_${CURRENT_DB_VERSION} " +
                    "добавлена в addMigrations(...). Исходная ошибка: ${e.message}",
                e
            )
        }

        try {
            // Открытие триггерит onUpgrade -> применяется MIGRATION_8_9.
            val version = db.openHelper.readableDatabase.version
            assertEquals(
                "Ожидалась версия $CURRENT_DB_VERSION после миграции",
                CURRENT_DB_VERSION,
                version
            )
        } finally {
            db.close()
        }
    }
}
