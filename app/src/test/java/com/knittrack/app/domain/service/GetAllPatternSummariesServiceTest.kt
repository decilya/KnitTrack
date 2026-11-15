package com.knittrack.app.domain.service

import com.knittrack.app.core.base.BaseTest
import com.knittrack.app.core.common.Result
import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.PatternName
import com.knittrack.app.domain.model.PatternSummary
import com.knittrack.app.domain.repository.PatternRepository
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
    fun executeReturnsFlowOfSummaries() = runTest {
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
