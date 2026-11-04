package com.knittrac.app.data.repository

import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.PatternDao
import com.knittrac.app.data.local.PatternRepeatDao
import com.knittrac.app.data.local.entity.PatternRepeatEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.entity.PatternOperation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

class BackupRepositoryImplTest {

    private val database: AppDatabase = mockk(relaxed = true)
    private val patternDao: PatternDao = mockk(relaxed = true)
    private val patternRepeatDao: PatternRepeatDao = mockk(relaxed = true)
    private val repository = BackupRepositoryImpl(database, mockk(), mockk())

    /**
     * Проверяет, что при импорте операции с несуществующим repeatCode 
     * он санитизируется (зануляется) перед вставкой в БД.
     */
    @Test
    fun savePatternOperationsBatch_sanitizesOrphanRepeatCode() = runTest {
        every { database.patternDao() } returns patternDao
        every { database.patternRepeatDao() } returns patternRepeatDao

        // 1. В БД есть строка с id=10 (привязана к паттерну 1) и валидный раппорт "A"
        coEvery { patternDao.getAllRows() } returns listOf(
            PatternRowEntity(id = 10L, patternId = 1L, rowNumber = 1)
        )
        coEvery { patternRepeatDao.getAllRepeats() } returns listOf(
            PatternRepeatEntity(patternId = 1L, code = "A", stitchCount = 2, rowCount = 1)
        )

        // 2. Приходит операция с "ORPHAN", которого нет в БД
        val symbol = Symbol("K", null, "Knit", "V", SymbolCategory.STANDARD)
        val opOrphan = PatternOperation(
            patternRowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0),
            symbol = symbol,
            repeatCode = "ORPHAN"
        )

        // 3. Перехватываем, что именно уйдёт в БД
        val capturedEntities = slot<List<com.knittrac.app.data.local.entity.PatternOperationEntity>>()
        coEvery { patternDao.insertOperations(capture(capturedEntities)) } returns Unit

        // Act
        repository.savePatternOperationsBatch(listOf(opOrphan))

        // Assert
        coVerify(exactly = 1) { patternDao.insertOperations(any()) }
        assertNull(capturedEntities.captured.first().repeatCode)
    }
}
