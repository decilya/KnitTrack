package com.knittrac.app.domain.service
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.DailyStat
import com.knittrac.app.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class GetProjectStatisticsServiceTest : BaseTest() {
    private val sessionRepository: SessionRepository = mockk()
    private val service = GetProjectStatisticsService(sessionRepository, coroutineRule.testDispatcher)
    @Test
    fun executeReturnsDailyStatsOnSuccess() = runTest {
        val stats = listOf(DailyStat(1000L, 3600L))
        coEvery { sessionRepository.getDailyStats(ProjectId.create(1L)) } returns Result.Success(stats)
        val result = service(ProjectId.create(1L))
        assertTrue(result is Result.Success)
        assertEquals(stats, (result as Result.Success).data)
        coVerify { sessionRepository.getDailyStats(ProjectId.create(1L)) }
    }
}
