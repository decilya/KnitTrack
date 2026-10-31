package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.model.PatternSummary
import com.knittrac.app.domain.repository.PatternRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [GetAllPatternSummariesService].
 */
class GetAllPatternSummariesServiceTest : BaseTest() {

    private val patternRepository: PatternRepository = mockk()
    private val service = GetAllPatternSummariesService(patternRepository, coroutineRule.testDispatcher)

    /**
     * Успех: сервис эмитит [Result.Success] со списком [PatternSummary].
     */
    @Test
    fun `execute returns flow of summaries`() = runTest {
        val summaries = listOf(
            PatternSummary(id = PatternId.create(1L), name = PatternName.create("A")),
            PatternSummary(id = PatternId.create(2L), name = PatternName.create("B"))
        )
        every { patternRepository.getAllPatternSummaries() } returns flowOf(summaries)

        val result = service(Unit).toList()

        assertEquals(1, result.size)
        assertTrue(result[0] is Result.Success)
        assertEquals(summaries, (result[0] as Result.Success).data)
    }
}
