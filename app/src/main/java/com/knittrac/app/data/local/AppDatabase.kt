package com.knittrac.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
}
