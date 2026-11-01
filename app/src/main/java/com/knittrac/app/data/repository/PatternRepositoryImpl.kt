package com.knittrac.app.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import com.knittrac.app.domain.model.PatternSummary
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.PatternDao
import com.knittrac.app.data.local.PatternRepeatDao
import com.knittrac.app.data.local.SymbolDao
import com.knittrac.app.data.local.entity.PatternOperationEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.data.mapper.toSummary
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.model.OperationSpan
import com.knittrac.app.domain.repository.PatternRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

/**
 * Реализация [PatternRepository], инкапсулирующая сборку агрегата [Pattern]
 * из пяти таблиц: `patterns`, `pattern_rows`, `pattern_operations`,
 * `symbols`, `pattern_repeats`.
 *
 * [getPatternById] делает 5 запросов. Операции — один SQL на схему, не N+1.
 * Для единичного вызова приемлемо (P2 в TECHDEBT: batch при списке).
 *
 * Все catch-блоки сначала отлавливают [CancellationException] и пробрасывают
 * её дальше: отмена корутины — не ошибка, а сигнал.
 */
class PatternRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val patternDao: PatternDao,
    private val symbolDao: SymbolDao,
    private val patternRepeatDao: PatternRepeatDao
) : PatternRepository {

    /**
     * Загружает схему по id вместе с рядами, операциями, легендой и раппортами.
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
            val operations = patternDao.getAllOperationsForPattern(id.value)
            val legend = symbols.map { it.toDomain() }.toSet()
            when (val attached = attachOperations(rows, legend, operations)) {
                is Result.Error -> attached
                is Result.Success -> Result.Success(
                    entity.toDomain(
                        rows = rows,
                        legend = legend,
                        repeats = repeats.map { it.toDomain() }
                    ).copy(rows = attached.data)
                )
            }
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
     * Клеит операции на ряды. Порядок внутри ряда — порядок списка DAO
     * (SQL `ORDER BY column_start`). Нет символа в легенде — [AppError.DatabaseError].
     * Нет изнаночного символа — `backSymbol = null`.
     */
    private fun attachOperations(
        rows: List<PatternRowEntity>,
        legend: Set<Symbol>,
        operations: List<PatternOperationEntity>
    ): Result<List<PatternRow>> {
        val byCode = legend.associateBy { it.code }
        val byRow = mutableMapOf<Long, MutableList<PatternOperation>>()
        for (entity in operations) {
            val symbol = byCode[entity.symbolCode]
                ?: return Result.Error(
                    AppError.DatabaseError(
                        IllegalStateException(
                            "Symbol '${entity.symbolCode}' not found in legend " +
                                "for operation row=${entity.patternRowId}"
                        )
                    )
                )
            val backSymbol = entity.backSymbolCode?.let { byCode[it] }
            byRow.getOrPut(entity.patternRowId) { mutableListOf() }
                .add(entity.toDomain(symbol, backSymbol))
        }
        return Result.Success(
            rows.map { rowEntity ->
                rowEntity.toDomain().copy(
                    operations = byRow[rowEntity.id] ?: emptyList()
                )
            }
        )
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
        val operations = entities.map { entity ->
            val symbol = symbols[entity.symbolCode]
                ?: return Result.Error(
                    AppError.DatabaseError(
                        IllegalStateException(
                            "Symbol '${entity.symbolCode}' not found in legend for pattern ${patternId.value}"
                        )
                    )
                )
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
    override suspend fun deleteRow(rowId: PatternRowId): Result<Unit> = try {
        val deletedRows = database.withTransaction {
            // 1. Сбрасываем прогресс в проектах, которые стоят на этом ряду,
            // чтобы не сработал триггер и не упало удаление.
            database.projectDao().clearProgressForRow(rowId.value)
            // 2. Удаляем сам ряд (операции удалятся каскадно).
            patternDao.deleteRow(rowId.value)
        }
        
        if (deletedRows == 0) {
            Result.Error(AppError.NotFoundError("Ряд id=${rowId.value} не найден"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "deleteRow rowId=%d failed", rowId.value)
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun updateOperation(operation: PatternOperation): Result<Unit> = try {
        val rows = patternDao.updateOperation(
            rowId = operation.patternRowId.value,
            columnStart = operation.columnStart.value,
            symbolCode = operation.symbol.code,
            backSymbolCode = operation.backSymbol?.code,
            colorHex = operation.colorHex,
            columnSpanOverride = operation.overrideSpan?.value,
            repeatCode = operation.repeatCode
        )
        
        // Если 0 строк, значит операции с таким PK не было
        if (rows == 0) {
            Result.Error(AppError.NotFoundError("Операция rowId=${operation.patternRowId.value} column=${operation.columnStart.value} не найдена"))
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "updateOperation rowId=%d column=%d failed", operation.patternRowId.value, operation.columnStart.value)
        Result.Error(AppError.DatabaseError(e))
    }

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

    override suspend fun deleteOperation(
        rowId: PatternRowId,
        columnStart: ColumnStart
    ): Result<Unit> = try {
        val rows = patternDao.deleteOperation(rowId.value, columnStart.value)
        if (rows == 0) {
            Result.Error(
                AppError.NotFoundError(
                    "Операция rowId=${rowId.value} column=${columnStart.value} не найдена"
                )
            )
        } else {
            Result.Success(Unit)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "deleteOperation rowId=%d column=%d failed", rowId.value, columnStart.value)
        Result.Error(AppError.DatabaseError(e))
    }

    override suspend fun addPattern(pattern: Pattern, rowCount: RowCount): Result<PatternId> = try {
        val id = database.withTransaction {
            val patternId = patternDao.insertPattern(pattern.toEntity())
            if (rowCount.value > 0) {
                patternDao.insertRows(emptyRows(patternId, rowCount.value))
            }
            PatternId.create(patternId)
        }
        Result.Success(id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "addPattern failed")
        Result.Error(AppError.DatabaseError(e))
    }

    private fun emptyRows(patternId: Long, count: Int): List<PatternRowEntity> =
        (1..count).map { index ->
            PatternRowEntity(id = 0L, patternId = patternId, rowNumber = index)
        }
}
