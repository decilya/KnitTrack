package com.knittrac.app.domain.service
import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.ProjectRepository
import com.knittrac.app.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class SaveSessionServiceTest : BaseTest() {
    private val sessionRepository: SessionRepository = mockk()
    private val projectRepository: ProjectRepository = mockk()
    private val service = SaveSessionService(sessionRepository, projectRepository, coroutineRule.testDispatcher)
    @Test
    fun `execute returns error when duration is zero`() = runTest {
        val result = service(SaveSessionParams(1L, 1000L, 1000L, 0))
        assertTrue(result is Result.Error)
        // Исправлено: добавлено уточнение в ожидаемое сообщение
        assertEquals("Пустая сессия (длительность <= 0)", (result as Result.Error).error.message)
        coVerify(exactly = 0) { sessionRepository.addSession(any()) }
        coVerify(exactly = 0) { projectRepository.updateTotalTime(any(), any()) }
    }
    @Test
    fun `execute does NOT call updateTotalTime when addSession fails`() = runTest {
        coEvery { sessionRepository.addSession(any()) } returns Result.Error(AppError.DatabaseError(Exception("DB fail")))
        val result = service(SaveSessionParams(1L, 1000L, 2000L, 5))
        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { projectRepository.updateTotalTime(any(), any()) }
    }
    @Test
    fun `execute returns success even if updateTotalTime fails (partial success)`() = runTest {
        coEvery { sessionRepository.addSession(any()) } returns Result.Success(100L)
        coEvery { projectRepository.updateTotalTime(1L, 1L) } returns Result.Error(AppError.DatabaseError(Exception("Update fail")))
        val result = service(SaveSessionParams(1L, 1000L, 2000L, 5))
        assertTrue(result is Result.Success)
        assertEquals(100L, (result as Result.Success).data)
        coVerify { projectRepository.updateTotalTime(1L, 1L) }
    }
}
