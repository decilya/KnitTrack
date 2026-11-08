package com.knittrac.app.domain.entity

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.model.MarkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTest {

    private fun project(
        patternId: PatternId? = PatternId.create(1L),
        currentPatternRowId: PatternRowId? = null,
        currentRowNumber: RowCount = RowCount.ZERO
    ): Project = Project(
        id = ProjectId.create(1L),
        name = ProjectName.create("Тест"),
        category = Category.KNITTING,
        createdAt = 0L,
        updatedAt = 0L,
        patternId = patternId,
        currentPatternRowId = currentPatternRowId,
        currentRowNumber = currentRowNumber,
        syncStatus = SyncStatus.PENDING
    )

    private fun row(id: Long = 1L): PatternRow = PatternRow(
        id = PatternRowId.create(id),
        rowNumber = RowNumber.create(1)
    )

    @Test
    fun markRow_returnsAlreadyExists_whenCurrentRowMatches() {
        val p = project(
            currentPatternRowId = PatternRowId.create(1L),
            currentRowNumber = RowCount.create(1)
        )
        val result = p.markRow(row(1L))
        assertTrue(result is Result.Success)
        assertEquals(MarkResult.AlreadyExists, (result as Result.Success).data)
    }

    @Test
    fun markRow_returnsAdded_whenCurrentRowDiffers() {
        val p = project(currentPatternRowId = PatternRowId.create(2L))
        val result = p.markRow(row(1L))
        assertTrue(result is Result.Success)
        assertEquals(MarkResult.Added, (result as Result.Success).data)
    }

    @Test
    fun markRow_returnsValidationError_whenPatternIdIsNull() {
        val p = project(patternId = null)
        val result = p.markRow(row(1L))
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun markRow_returnsValidationError_whenStitchPositiveWithoutRow() {
        val project = Project(
            id = ProjectId.create(1L),
            name = ProjectName.create("Тест"),
            category = Category.KNITTING,
            createdAt = 0L,
            updatedAt = 0L,
            syncStatus = SyncStatus.PENDING,
            patternId = PatternId.create(1L),
            currentPatternRowId = null,
            currentStitch = StitchCount.create(5)
        )
        val result = project.markRow(PatternRow(
            id = PatternRowId.create(1L),
            rowNumber = RowNumber.create(1)
        ))
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * D-инвариант не срабатывает, когда ряд установлен: currentStitch > 0
     * при currentPatternRowId != null — валидное состояние.
     */
    @Test
    fun markRow_returnsAdded_whenStitchPositiveAndRowSet() {
        val project = Project(
            id = ProjectId.create(1L),
            name = ProjectName.create("Тест"),
            category = Category.KNITTING,
            createdAt = 0L,
            updatedAt = 0L,
            syncStatus = SyncStatus.PENDING,
            patternId = PatternId.create(1L),
            currentPatternRowId = PatternRowId.create(1L),
            currentStitch = StitchCount.create(5)
        )
        val result = project.markRow(PatternRow(
            id = PatternRowId.create(2L),
            rowNumber = RowNumber.create(2)
        ))
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data == MarkResult.Added)
    }
}
