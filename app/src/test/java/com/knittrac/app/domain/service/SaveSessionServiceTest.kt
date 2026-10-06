package com.knittrac.app.domain.service

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

/**
 * Юнит-тесты для [SaveSessionService].
 * Проверяют реализацию Правила 7 ТЗ: валидация, сохранение сессии и обновление времени проекта.
 * 
 * Конструктор сервиса принимает (sessionRepository, projectRepository).
 */
class SaveSessionServiceTest {

    private val sessionRepository: SessionRepository = mockk()
    private val projectRepository: ProjectRepository = mockk()
    private val service = SaveSessionService(sessionRepository, projectRepository)

    /**
     * Проверяет, что сессия с нулевой или отрицательной длительностью не сохраняется.
     */
    @Test
    fun `invoke returns validation error when duration is zero`() = runTest {
        // Arrange
        val params = SaveSessionParams(
            projectId = 1L,
            startTimestamp = 1_000L,
            endTimestamp = 1_000L,
            rowCount = 0
        )

        // Act
        val result = service(params)

        // Assert
        assertTrue(result is Result.Error)
        assertEquals("Пустая сессия", (result as Result.Error).error.message)
        coVerify(exactly = 0) { sessionRepository.addSession(any()) }
        coVerify(exactly = 0) { projectRepository.updateTotalTime(any(), any()) }
    }

    /**
     * Проверяет успешный сценарий: сессия сохранена, время проекта обновлено.
     */
    @Test
    fun `invoke saves session and updates total time on success`() = runTest {
        // Arrange
        val params = SaveSessionParams(
            projectId = 1L,
            startTimestamp = 1_000L,
            endTimestamp = 5_000L,
            rowCount = 5
        )
        coEvery { sessionRepository.addSession(any()) } returns Result.Success(10L)
        coEvery { projectRepository.updateTotalTime(1L, 4L) } returns Result.Success(Unit)

        // Act
        val result = service(params)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(10L, (result as Result.Success).data)
        coVerify { sessionRepository.addSession(any()) }
        coVerify { projectRepository.updateTotalTime(1L, 4L) }
    }

    /**
     * Проверяет, что при ошибке сохранения сессии обновление времени проекта НЕ вызывается.
     */
    @Test
    fun `invoke does NOT call updateTotalTime when addSession fails`() = runTest {
        // Arrange
        val params = SaveSessionParams(
            projectId = 1L,
            startTimestamp = 1_000L,
            endTimestamp = 5_000L,
            rowCount = 5
        )
        coEvery { sessionRepository.addSession(any()) } returns
            Result.Error(AppError.DatabaseError(Exception("DB fail")))

        // Act
        val result = service(params)

        // Assert
        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { projectRepository.updateTotalTime(any(), any()) }
    }

    /**
     * Проверяет сценарий частичного успеха: сессия сохранена, но обновление времени проекта упало.
     * Сервис должен вернуть Success (ID сессии), так как основная операция выполнена.
     */
    @Test
    fun `invoke returns success even if updateTotalTime fails (partial success)`() = runTest {
        // Arrange
        val params = SaveSessionParams(
            projectId = 1L,
            startTimestamp = 1_000L,
            endTimestamp = 5_000L,
            rowCount = 5
        )
        coEvery { sessionRepository.addSession(any()) } returns Result.Success(10L)
        coEvery { projectRepository.updateTotalTime(1L, 4L) } returns
            Result.Error(AppError.DatabaseError(Exception("Update fail")))

        // Act
        val result = service(params)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(10L, (result as Result.Success).data)
        coVerify { projectRepository.updateTotalTime(1L, 4L) }
    }
}
