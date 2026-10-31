package com.knittrac.app.data.repository

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import com.knittrac.app.domain.model.PatternSummary
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.PatternDao
import com.knittrac.app.data.local.PatternRepeatDao
import com.knittrac.app.data.local.SymbolDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.data.mapper.toSummary
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.model.OperationSpan
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

    /**
     * Возвращает интервалы операций в ряду для overlap-check.
     *
     * Проекция `OperationSpanProjection` мапится в Domain-модель [OperationSpan]
     * с VO-типами ([ColumnStart], [Span]).
     */
    override suspend fun getOperationSpansForRow(
        rowId: PatternRowId
    ): Result<List<OperationSpan>> = try {
        val projections = patternDao.getOperationSpansForRow(rowId.value)
        Result.Success(
            projections.map {
                OperationSpan(
                    columnStart = ColumnStart.createOrDefault(it.columnStart),
                    span = Span.createOrDefault(it.effectiveSpan)
                )
            }
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "getOperationSpansForRow rowId=%d failed", rowId.value)
        Result.Error(AppError.DatabaseError(e))
    }

    /**
     * Загружает операции ряда и резолвит символы через legend схемы.
     *
     * `pattern_operations.symbol_code` — FK на `symbols.code`. Чтобы смапить
     * `PatternOperationEntity` → `PatternOperation` (с полным `Symbol`),
     * загружаем legend схемы (`symbolDao.getSymbolsForPattern`) и делаем
     * lookup по `code`.
     */
    override suspend fun getOperationsForRow(
        patternId: PatternId,
        rowId: PatternRowId
    ): Result<List<PatternOperation>> = try {
        val symbols = symbolDao.getSymbolsForPattern(patternId.value)
            .map { it.toDomain() }
            .associateBy { it.code }
        val entities = patternDao.getOperationsForRow(rowId.value)
        val operations = entities.mapNotNull { entity ->
            val symbol = symbols[entity.symbolCode]
                ?: return@mapNotNull null  // FK RESTRICT гарантирует наличие; defensive
            val backSymbol = entity.backSymbolCode?.let { symbols[it] }
            entity.toDomain(symbol, backSymbol)
        }
        Result.Success(operations)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "getOperationsForRow patternId=%d rowId=%d failed", patternId.value, rowId.value)
        Result.Error(AppError.DatabaseError(e))
    }

    override fun getAllPatternSummaries(): Flow<List<PatternSummary>> =
        patternDao.getAllPatternsFlow().map { entities ->
            entities.map { it.toSummary() }
        }

    /**
     * Вставляет операцию в ряд через пакетный [PatternDao.insertOperations].
     */
    override suspend fun addOperation(operation: PatternOperation): Result<Unit> = try {
        patternDao.insertOperations(listOf(operation.toEntity()))
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(
            e,
            "addOperation rowId=%d columnStart=%d failed",
            operation.patternRowId.value,
            operation.columnStart.value
        )
        Result.Error(AppError.DatabaseError(e))
    }
}
