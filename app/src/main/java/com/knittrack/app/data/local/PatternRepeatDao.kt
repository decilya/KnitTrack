package com.knittrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.knittrack.app.data.local.entity.PatternRepeatEntity

/** DAO для работы с раппортами схем (повторяющимися фрагментами). */
@Dao
interface PatternRepeatDao {

    /**
     * Возвращает раппорты указанной схемы.
     *
     * Раппорт идентифицируется парой (pattern_id, code); порядок
     * не семантичен (справочник, не последовательность).
     *
     * @param patternId Идентификатор схемы.
     * @return Список раппортов схемы (может быть пустым).
     */
    @Query("SELECT * FROM pattern_repeats WHERE pattern_id = :patternId")
    suspend fun getRepeatsForPattern(patternId: Long): List<PatternRepeatEntity>

    @Query("SELECT * FROM pattern_repeats ORDER BY pattern_id ASC, code ASC")
    suspend fun getAllRepeats(): List<PatternRepeatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepeats(repeats: List<PatternRepeatEntity>)

    /**
     * Вставка раппорта. ABORT при конфликте по композитному PK
     * (pattern_id, code). Используется только внутри [upsertRepeats].
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRepeatOrThrow(repeat: PatternRepeatEntity)

    /** Обновление раппорта по PK. @return количество затронутых строк. */
    @Update
    suspend fun updateRepeat(repeat: PatternRepeatEntity): Int

    /**
     * Upsert списка раппортов для импорта (UPDATE по PK; при 0 — INSERT).
     */
    @Transaction
    suspend fun upsertRepeats(repeats: List<PatternRepeatEntity>) {
        for (r in repeats) {
            if (updateRepeat(r) == 0) insertRepeatOrThrow(r)
        }
    }
}
