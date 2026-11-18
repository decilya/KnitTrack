package com.knittrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.knittrack.app.data.local.entity.SymbolEntity

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

    /**
     * Вставка символа. ABORT при конфликте по PK (code).
     * Используется только внутри [upsertSymbols].
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSymbolOrThrow(symbol: SymbolEntity)

    /** Обновление символа по PK. @return количество затронутых строк. */
    @Update
    suspend fun updateSymbol(symbol: SymbolEntity): Int

    /**
     * Upsert списка символов для импорта (UPDATE по PK; при 0 — INSERT ABORT).
     *
     * Не использует DELETE, поэтому не срабатывает FK RESTRICT
     * (pattern_operations.symbol_code -> symbols ON DELETE RESTRICT):
     * импорт частичного бэкапа, в котором нет системного символа,
     * используемого в локальной схеме, больше не падает с
     * SQLiteConstraintException.
     */
    @Transaction
    suspend fun upsertSymbols(symbols: List<SymbolEntity>) {
        for (s in symbols) {
            if (updateSymbol(s) == 0) insertSymbolOrThrow(s)
        }
    }

    @Query("DELETE FROM symbols WHERE code = :code AND owner_pattern_id = :patternId")
    suspend fun deleteSymbol(code: String, patternId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbols(symbols: List<SymbolEntity>)
}
