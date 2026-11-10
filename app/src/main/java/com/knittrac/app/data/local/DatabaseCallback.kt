package com.knittrac.app.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Callback для fresh install: создает CHECK constraints, триггеры и сидеры.
 *
 * Вынесен из [AppDatabase] для соблюдения SRP (Single Responsibility Principle).
 */
internal object DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL("PRAGMA foreign_keys = OFF")
        db.execSQL("DROP TABLE sessions")
        db.execSQL("DROP TABLE projects")
        db.execSQL(AppDatabaseMigrations.CREATE_PROJECTS_V10)
        db.execSQL(AppDatabaseMigrations.CREATE_INDEX_PROJECTS_PATTERN_ID)
        db.execSQL(AppDatabaseMigrations.CREATE_INDEX_PROJECTS_CURRENT_ROW)
        db.execSQL("ALTER TABLE projects_new RENAME TO projects")
        db.execSQL(AppDatabaseMigrations.CREATE_SESSIONS_WITH_CHECK)
        db.execSQL(AppDatabaseMigrations.CREATE_INDEX_SESSIONS_PROJECT_ID)
        db.execSQL(AppDatabaseMigrations.CREATE_INDEX_SESSIONS_START_TIME)
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL(AppDatabaseMigrations.CREATE_TRIGGER_PROGRESS_INSERT_V10)
        db.execSQL(AppDatabaseMigrations.CREATE_TRIGGER_PROGRESS_UPDATE_V10)
        db.execSQL(AppDatabaseMigrations.CREATE_TRIGGER_REPEAT_INSERT_V9)
        db.execSQL(AppDatabaseMigrations.CREATE_TRIGGER_REPEAT_UPDATE_V9)
        db.execSQL(AppDatabaseMigrations.DEFAULT_SYMBOLS_INSERT)
    }
}
