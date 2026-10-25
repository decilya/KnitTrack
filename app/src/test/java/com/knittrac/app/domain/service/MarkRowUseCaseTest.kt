package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseTest
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

/**
 * Юнит-тесты для [MarkRowUseCase].
 *
 * Проверяют цепочку: загрузка [Project] → загрузка [Pattern] → поиск
 * [PatternRow] по id → делегирование в [ProgressService.markRow],
 * а также обработку ошибок на каждом шаге.
 */
class MarkRowUseCaseTest : BaseTest() {

    private val projectRepository: ProjectRepository = mockk()
    private val patternRepository: PatternRepository = mockk()
    private val progressService: ProgressService = mockk()
    private val useCase = MarkRowUseCase(
        projectRepository,
        patternRepository,
        progressService,
        coroutineRule.testDispatcher
    )

    private fun project(patternId: PatternId? = PatternId.create(1L)): Project = Project(
        id = 1L,
        name = ProjectName.create("Тест"),
        category = Category.KNITTING,
        createdAt = 0L,
        updatedAt = 0L,
        patternId = patternId,
        syncStatus = SyncStatus.PENDING
    )

    private fun row(id: Long = 10L): PatternRow = PatternRow(
        id = PatternRowId.create(id),
        rowNumber = RowNumber.create(1)
    )

    private fun pattern(rows: List<PatternRow> = listOf(row())): Pattern = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false,
        rows = rows
    )

    private fun params(rowId: Long = 10L): MarkRowParams =
        MarkRowParams(projectId = 1L, rowId = PatternRowId.create(rowId))

    /**
     * Успешный путь: project + pattern + row найдены → ProgressService вернул Added.
     */
    @Test
    fun `execute returns Added when all aggregates found`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project())
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { progressService.markRow(any(), any()) } returns Result.Success(MarkResult.Added)

        val result = useCase(params())

        assertTrue(result is Result.Success)
        assertEquals(MarkResult.Added, (result as Result.Success).data)
        coVerify { progressService.markRow(any(), any()) }
    }

    /**
     * ProgressService вернул AlreadyExists — пробрасываем как Success.
     */
    @Test
    fun `execute returns AlreadyExists from ProgressService`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project())
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { progressService.markRow(any(), any()) } returns
            Result.Success(MarkResult.AlreadyExists)

        val result = useCase(params())

        assertTrue(result is Result.Success)
        assertEquals(MarkResult.AlreadyExists, (result as Result.Success).data)
    }

    /**
     * Проект не найден — NotFoundError без обращения к pattern/ProgressService.
     */
    @Test
    fun `execute returns NotFoundError when project missing`() = runTest {
        coEvery { projectRepository.getProjectById(99L) } returns
            Result.Error(AppError.NotFoundError("нет"))

        val result = useCase(MarkRowParams(projectId = 99L, rowId = PatternRowId.create(10L)))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    /**
     * У проекта нет patternId — ValidationError.
     */
    @Test
    fun `execute returns ValidationError when project has no pattern`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project(patternId = null))

        val result = useCase(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * PatternRepository вернул ошибку — пробрасываем.
     */
    @Test
    fun `execute returns pattern repository error`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project())
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Error(AppError.DatabaseError(RuntimeException("db")))

        val result = useCase(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }

    /**
     * Ряд не найден в pattern.rows — ValidationError.
     */
    @Test
    fun `execute returns ValidationError when row missing in pattern`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project())
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Success(pattern(rows = emptyList()))

        val result = useCase(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * ProgressService вернул ошибку — пробрасываем.
     */
    @Test
    fun `execute returns ProgressService error`() = runTest {
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project())
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern())
        coEvery { progressService.markRow(any(), any()) } returns
            Result.Error(AppError.ValidationError("чужой ряд"))

        val result = useCase(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }
}
