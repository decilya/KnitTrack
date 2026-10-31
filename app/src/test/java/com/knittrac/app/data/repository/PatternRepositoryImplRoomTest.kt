package com.knittrac.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternOperationEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.data.local.entity.SymbolEntity
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.Technique
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Round-trip агрегата через Room.
 *
 * `UnconfinedTestDispatcher`: без `advanceUntilIdle`, потому что операции БД
 * в тесте синхронные (`allowMainThreadQueries`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PatternRepositoryImplRoomTest {

    @Test
    fun getPatternById_returnsOperationsOnTheirRow() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DATABASE_CALLBACK)
            .build()
        try {
            db.symbolDao().insertSymbols(
                listOf(
                    SymbolEntity(
                        code = "K",
                        displayName = "Лицевая",
                        textGlyph = "V",
                        category = "STANDARD"
                    )
                )
            )
            db.patternDao().insertPattern(
                PatternEntity(
                    id = 1L,
                    name = "Схема",
                    technique = "KNITTING",
                    direction = "LTR",
                    isReversible = false
                )
            )
            db.patternDao().insertRows(
                listOf(
                    PatternRowEntity(id = 10L, patternId = 1L, rowNumber = 1),
                    PatternRowEntity(id = 11L, patternId = 1L, rowNumber = 2)
                )
            )
            db.patternDao().insertOperations(
                listOf(PatternOperationEntity(patternRowId = 10L, columnStart = 0, symbolCode = "K"))
            )
            val repository = PatternRepositoryImpl(
                db,
                db.patternDao(),
                db.symbolDao(),
                db.patternRepeatDao()
            )

            val result = repository.getPatternById(PatternId.create(1L))

            assertTrue(result is Result.Success)
            val pattern = (result as Result.Success).data
            val row10 = pattern.rows.first { it.id.value == 10L }
            val row11 = pattern.rows.first { it.id.value == 11L }
            assertEquals(1, row10.operations.size)
            assertEquals(0, row10.operations[0].columnStart.value)
            assertEquals("K", row10.operations[0].symbol.code)
            assertTrue(row11.operations.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun addPattern_insertsPatternAndNumberedRows() = runTest(UnconfinedTestDispatcher()) {
        val db = openDb()
        try {
            val repository = repository(db)
            val result = repository.addPattern(newPattern(), RowCount.create(2))
            assertTrue(result is Result.Success)
            val id = (result as Result.Success).data
            val rows = db.patternDao().getRowsForPattern(id.value)
            assertEquals(listOf(1, 2), rows.map { it.rowNumber })
            assertTrue(rows.all { it.id != 0L })
        } finally {
            db.close()
        }
    }

    @Test
    fun addPattern_writeFailure_returnsDatabaseError() = runTest(UnconfinedTestDispatcher()) {
        val db = openDb()
        try {
            val repository = repository(db)
            db.openHelper.writableDatabase.execSQL("DROP TABLE patterns")
            val result = repository.addPattern(newPattern(), RowCount.create(1))
            assertTrue(result.toString(), result is Result.Error)
            assertTrue((result as Result.Error).error is AppError.DatabaseError)
        } finally {
            db.close()
        }
    }

    @Test
    fun deleteOperation_realRoom_deletesRow() = runTest(UnconfinedTestDispatcher()) {
        val db = openDb()
        try {
            db.patternDao().insertPattern(
                PatternEntity(
                    id = 1L,
                    name = "Схема",
                    technique = "KNITTING",
                    direction = "LTR",
                    isReversible = false
                )
            )
            db.patternDao().insertRows(
                listOf(PatternRowEntity(id = 10L, patternId = 1L, rowNumber = 1))
            )
            db.patternDao().insertOperations(
                listOf(PatternOperationEntity(patternRowId = 10L, columnStart = 3, symbolCode = "K"))
            )
            val result = repository(db).deleteOperation(
                PatternRowId.create(10L),
                ColumnStart.create(3)
            )
            assertTrue(result is Result.Success)
            assertTrue(db.patternDao().getAllOperations().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun deleteOperation_realRoom_missing_returnsNotFoundError() = runTest(UnconfinedTestDispatcher()) {
        val db = openDb()
        try {
            val result = repository(db).deleteOperation(
                PatternRowId.create(10L),
                ColumnStart.create(0)
            )
            assertTrue(result is Result.Error)
            assertTrue((result as Result.Error).error is AppError.NotFoundError)
        } finally {
            db.close()
        }
    }

    private fun openDb(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DATABASE_CALLBACK)
            .build()
    }

    private fun repository(db: AppDatabase): PatternRepositoryImpl = PatternRepositoryImpl(
        db,
        db.patternDao(),
        db.symbolDao(),
        db.patternRepeatDao()
    )

    private fun newPattern(): Pattern = Pattern(
        id = PatternId.ZERO,
        name = PatternName.create("Новая"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false
    )
}
