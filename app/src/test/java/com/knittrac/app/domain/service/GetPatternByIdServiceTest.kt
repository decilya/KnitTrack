package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.Technique
import com.knittrac.app.domain.repository.PatternRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [GetPatternByIdService].
 */
class GetPatternByIdServiceTest : BaseTest() {

    private val patternRepository: PatternRepository = mockk()
    private val service = GetPatternByIdService(patternRepository, coroutineRule.testDispatcher)

    /**
     * Успех: сервис возвращает [Result.Success] с полным [Pattern].
     */
    @Test
    fun `execute returns pattern from repository`() = runTest {
        val pattern = Pattern(
            id = PatternId.create(1L),
            name = PatternName.create("Схема"),
            technique = Technique.KNITTING,
            direction = PatternDirection.LTR,
            isReversible = false
        )
        coEvery { patternRepository.getPatternById(any()) } returns Result.Success(pattern)

        val result = service(PatternId.create(1L))

        assertTrue(result is Result.Success)
        assertEquals(pattern, (result as Result.Success).data)
    }

    /**
     * Ошибка: сервис пробрасывает [Result.Error] из репозитория.
     */
    @Test
    fun `execute propagates NotFoundError`() = runTest {
        coEvery { patternRepository.getPatternById(any()) } returns
            Result.Error(AppError.NotFoundError("нет"))

        val result = service(PatternId.create(999L))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }
}
