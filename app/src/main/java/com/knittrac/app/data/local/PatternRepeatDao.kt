package com.knittrac.app.data.local

import androidx.room.Dao
import androidx.room.Query
import com.knittrac.app.data.local.entity.PatternRepeatEntity

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
}
