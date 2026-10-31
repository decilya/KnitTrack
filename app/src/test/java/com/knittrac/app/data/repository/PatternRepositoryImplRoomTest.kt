package com.knittrac.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternOperationEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.data.local.entity.SymbolEntity
import com.knittrac.app.domain.entity.PatternId
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
}
