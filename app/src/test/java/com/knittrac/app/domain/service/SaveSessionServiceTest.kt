package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.FakeTimeProvider
import com.knittrac.app.domain.repository.SessionRepository
import com.knittrac.app.domain.entity.SessionId
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.RowCount
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [SaveSessionService].
 * Проверяют валидацию входных данных и корректное делегирование атомарного сохранения в репозиторий.
 */
class SaveSessionServiceTest {

    private val sessionRepository: SessionRepository = mockk()
    private val timeProvider = FakeTimeProvider()
    private val service = SaveSessionService(sessionRepository, timeProvider)

    /**
     * Проверяет, что сессия с нулевой или отрицательной длительностью отвергается на этапе валидации.
     */
    @Test
    fun `invoke returns validation error when duration is zero`() = runTest {
        // Arrange
        val params = SaveSessionParams(projectId = ProjectId.create(1L), startTimestamp = 1_000L, endTimestamp = 1_000L, rowCount = RowCount.ZERO)
        
        // Act
        val result = service(params)
        
        // Assert
        assertTrue(result is Result.Error)
        assertEquals("Пустая сессия", (result as Result.Error).error.message)
        coVerify(exactly = 0) { sessionRepository.addSessionAtomically(any()) }
    }

    /**
     * Проверяет успешный сценарий: валидные параметры передаются в атомарный метод репозитория.
     */
    @Test
    fun `invoke delegates to atomic repository method on success`() = runTest {
        // Arrange
        val params = SaveSessionParams(projectId = ProjectId.create(1L), startTimestamp = 1_000L, endTimestamp = 5_000L, rowCount = RowCount.create(5))
        coEvery { sessionRepository.addSessionAtomically(any()) } returns Result.Success(SessionId.create(10L))
        
        // Act
        val result = service(params)
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(SessionId.create(10L), (result as Result.Success).data)
        coVerify { sessionRepository.addSessionAtomically(any()) }
    }

    /**
     * Проверяет, что ошибка репозитория корректно прокидывается наверх.
     */
    @Test
    fun `invoke returns error when atomic repository method fails`() = runTest {
        // Arrange
        val params = SaveSessionParams(projectId = ProjectId.create(1L), startTimestamp = 1_000L, endTimestamp = 5_000L, rowCount = RowCount.create(5))
        coEvery { sessionRepository.addSessionAtomically(any()) } returns 
            Result.Error(AppError.DatabaseError(Exception("DB constraint fail")))
        
        // Act
        val result = service(params)
        
        // Assert
        assertTrue(result is Result.Error)
        coVerify { sessionRepository.addSessionAtomically(any()) }
    }
}
