package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.PatternDao
import com.knittrac.app.data.local.PatternRepeatDao
import com.knittrac.app.data.local.SymbolDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.repository.PatternRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

/**
 * Реализация [PatternRepository], инкапсулирующая сборку агрегата [Pattern]
 * из четырёх таблиц: `patterns`, `pattern_rows`, `symbols`, `pattern_repeats`.
 *
 * ВАЖНО: агрегат [Pattern] в Domain содержит `legend` и `repeats` — без них
 * невозможны проверки `symbol in legend` и `repeat_code in pattern.repeats`
 * в [AddPatternOperationService] (Шаг 7d). Поэтому [getPatternById] делает
 * 4 запроса. Для единичного вызова приемлемо (P2 в TECHDEBT: batch при списке).
 *
 * Все catch-блоки сначала отлавливают [CancellationException] и пробрасывают
 * её дальше: отмена корутины — не ошибка, а сигнал.
 */
class PatternRepositoryImpl @Inject constructor(
    private val patternDao: PatternDao,
    private val symbolDao: SymbolDao,
    private val patternRepeatDao: PatternRepeatDao
) : PatternRepository {

    /**
     * Загружает схему по id вместе с рядами, легендой и раппортами.
     *
     * @param id Идентификатор схемы.
     * @return [Result.Success] с полным [Pattern],
     *   [Result.Error] с [AppError.NotFoundError] если схемы нет,
     *   или [AppError.DatabaseError] при сбое БД.
     */
    override suspend fun getPatternById(id: PatternId): Result<Pattern> = try {
        val entity = patternDao.getPatternById(id.value)
        if (entity != null) {
            val rows = patternDao.getRowsForPattern(id.value)
            val symbols = symbolDao.getSymbolsForPattern(id.value)
            val repeats = patternRepeatDao.getRepeatsForPattern(id.value)
            Result.Success(
                entity.toDomain(
                    rows = rows,
                    legend = symbols.map { it.toDomain() }.toSet(),
                    repeats = repeats.map { it.toDomain() }
                )
            )
        } else {
            Result.Error(AppError.NotFoundError("Схема id=${id.value} не найдена"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "getPatternById id=%d failed", id.value)
        Result.Error(AppError.DatabaseError(e))
    }
}
