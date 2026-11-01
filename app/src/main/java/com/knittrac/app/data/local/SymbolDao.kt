package com.knittrac.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.knittrac.app.data.local.entity.SymbolEntity

/**
 * DAO для работы с символами схем.
 *
 * Символы делятся на два вида:
 * - системные (`owner_pattern_id IS NULL`) — глобальный справочник, доступны всем схемам;
 * - кастомные (`owner_pattern_id = :patternId`) — принадлежат одной схеме.
 *
 * Легенда схемы — объединение обоих видов (см. [getSymbolsForPattern]).
 */
@Dao
interface SymbolDao {

    /**
     * Возвращает символы, доступные схеме [patternId].
     *
     * Включает системные (owner_pattern_id = NULL) и кастомные
     * (owner_pattern_id = patternId) символы. Другие схемы не видны.
     *
     * @param patternId Идентификатор схемы.
     * @return Список символов (системные + кастомные) без гарантии порядка.
     */
    @Query("SELECT * FROM symbols WHERE owner_pattern_id IS NULL OR owner_pattern_id = :patternId")
    suspend fun getSymbolsForPattern(patternId: Long): List<SymbolEntity>

    @Query("SELECT * FROM symbols ORDER BY code ASC")
    suspend fun getAllSymbols(): List<SymbolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbol(symbol: SymbolEntity)

    @Query("DELETE FROM symbols WHERE code = :code AND owner_pattern_id = :patternId")
    suspend fun deleteSymbol(code: String, patternId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbols(symbols: List<SymbolEntity>)
}
