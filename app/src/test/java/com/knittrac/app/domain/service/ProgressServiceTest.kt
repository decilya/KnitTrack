package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.model.MarkResult
import com.knittrac.app.domain.repository.PatternRepository
import com.knittrac.app.domain.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressServiceTest {

    private val patternRepository: PatternRepository = mockk()
    private val projectRepository: ProjectRepository = mockk()
    private val service = ProgressService(patternRepository, projectRepository)

    private fun project(
        patternId: PatternId? = PatternId.create(1L),
        currentPatternRowId: PatternRowId? = PatternRowId.create(2L)
    ): Project = Project(
        id = 1L,
        name = ProjectName.create("Тест"),
        category = Category.KNITTING,
        createdAt = 0L,
        updatedAt = 0L,
        patternId = patternId,
        currentPatternRowId = currentPatternRowId,
        syncStatus = SyncStatus.PENDING
    )

    private fun row(id: Long = 1L, number: Int = 1): PatternRow = PatternRow(
        id = PatternRowId.create(id),
        rowNumber = RowNumber.create(number)
    )

    private fun pattern(rows: List<PatternRow>): Pattern = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        rows = rows
    )

    @Test
    fun markRow_success_added_callsUpdateProjectProgress() = runTest {
        val p = project()
        val r = row(1L)
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern(listOf(r)))
        coEvery { projectRepository.updateProjectProgress(any(), any(), any(), any()) } returns Result.Success(Unit)

        val result = service.markRow(p, r)

        assertTrue(result is Result.Success)
        assertEquals(MarkResult.Added, (result as Result.Success).data)
        coVerify { projectRepository.updateProjectProgress(any(), any(), any(), any()) }
    }

    @Test
    fun markRow_alreadyExists_doesNotCallUpdate() = runTest {
        val p = project(currentPatternRowId = PatternRowId.create(1L))
        val r = row(1L)

        val result = service.markRow(p, r)

        assertTrue(result is Result.Success)
        assertEquals(MarkResult.AlreadyExists, (result as Result.Success).data)
        coVerify(exactly = 0) { projectRepository.updateProjectProgress(any(), any(), any(), any()) }
    }

    @Test
    fun markRow_patternIdNull_returnsValidationError() = runTest {
        val p = project(patternId = null)
        val r = row(1L)

        val result = service.markRow(p, r)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun markRow_rowNotInPattern_returnsValidationError() = runTest {
        val p = project()
        val r = row(1L)
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern(emptyList()))

        val result = service.markRow(p, r)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    @Test
    fun markRow_patternRepositoryError_propagates() = runTest {
        val p = project()
        val r = row(1L)
        coEvery { patternRepository.getPatternById(any()) } returns Result.Error(AppError.NotFoundError("нет"))

        val result = service.markRow(p, r)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    @Test
    fun markRow_updateProjectProgressError_propagates() = runTest {
        val p = project()
        val r = row(1L)
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern(listOf(r)))
        coEvery { projectRepository.updateProjectProgress(any(), any(), any(), any()) } returns
            Result.Error(AppError.DatabaseError(RuntimeException("db")))

        val result = service.markRow(p, r)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }

    // ============================================================
    // markNextRow
    // ============================================================

    /**
     * У проекта нет currentPatternRowId — берётся первый ряд схемы.
     */
    @Test
    fun markNextRow_firstRow_whenNoCurrent() = runTest {
        val firstRow = row(id = 10L, number = 1)
        val p = project(currentPatternRowId = null)
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Success(pattern(listOf(firstRow)))
        coEvery { projectRepository.updateProjectProgress(any(), any(), any(), any()) } returns
            Result.Success(Unit)

        val result = service.markNextRow(p)

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.value)
    }

    /**
     * Есть currentPatternRowId → выбирается ряд rowNumber + 1.
     */
    @Test
    fun markNextRow_advancesToNext() = runTest {
        val r1 = row(id = 10L, number = 1)
        val r2 = row(id = 11L, number = 2)
        val r3 = row(id = 12L, number = 3)
        val p = project(currentPatternRowId = r2.id)
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Success(pattern(listOf(r1, r2, r3)))
        coEvery { projectRepository.updateProjectProgress(any(), any(), any(), any()) } returns
            Result.Success(Unit)

        val result = service.markNextRow(p)

        assertTrue(result is Result.Success)
        assertEquals(3, (result as Result.Success).data.value)
    }

    /**
     * Текущий ряд — последний: ValidationError, без записи в БД.
     */
    @Test
    fun markNextRow_lastRow_returnsValidationError() = runTest {
        val r1 = row(id = 10L, number = 1)
        val r2 = row(id = 11L, number = 2)
        val p = project(currentPatternRowId = r2.id)
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Success(pattern(listOf(r1, r2)))

        val result = service.markNextRow(p)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * Пустые rows — ValidationError.
     */
    @Test
    fun markNextRow_emptyRows_returnsValidationError() = runTest {
        val p = project(currentPatternRowId = null)
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Success(pattern(emptyList()))

        val result = service.markNextRow(p)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * patternId == null — ValidationError (нет привязанной схемы).
     */
    @Test
    fun markNextRow_patternIdNull_returnsValidationError() = runTest {
        val p = project(patternId = null, currentPatternRowId = null)

        val result = service.markNextRow(p)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * currentPatternRowId не найден в rows — ValidationError (рассинхронизация).
     */
    @Test
    fun markNextRow_currentRowMissing_returnsValidationError() = runTest {
        val r1 = row(id = 10L, number = 1)
        val p = project(currentPatternRowId = PatternRowId.create(999L))
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Success(pattern(listOf(r1)))

        val result = service.markNextRow(p)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }
}
