package com.knittrac.app.data.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.Session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [SessionRepositoryImpl].
 * Проверяют корректный маппинг Entity → Domain при работе с сессиями.
 */
class SessionRepositoryImplTest {

    private val sessionDao: SessionDao = mockk()
    private val repository = SessionRepositoryImpl(sessionDao)

    private val testEntity = SessionEntity(
        id = 1L,
        projectId = 10L,
        startTimestamp = 1_000L,
        endTimestamp = 61_000L,
        durationSeconds = 60L,
        rowCount = 5,
        updatedAt = 62_000L,
        syncStatus = "PENDING"
    )

    /**
     * Проверяет, что получение сессий по ID проекта возвращает корректно замапленные Domain-модели.
     */
    @Test
    fun `getSessionsByProjectId should map entities to domain`() = runTest {
        // Arrange
        coEvery { sessionDao.getSessionsByProjectId(10L) } returns listOf(testEntity)

        // Act
        val result = repository.getSessionsByProjectId(10L)

        // Assert
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(60L, list[0].durationSeconds)
        assertEquals(5, list[0].rowCount)
        coVerify { sessionDao.getSessionsByProjectId(10L) }
    }

    /**
     * Проверяет, что добавление сессии корректно маппит Domain в Entity и возвращает ID.
     */
    @Test
    fun `addSession should map domain to entity and return id`() = runTest {
        // Arrange
        coEvery { sessionDao.insertSession(any()) } returns 100L
        val session = Session(
            projectId = 10L,
            startTimestamp = 1_000L,
            endTimestamp = 61_000L,
            durationSeconds = 60L,
            rowCount = 5
        )

        // Act
        val result = repository.addSession(session)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(100L, (result as Result.Success).data)
        coVerify { sessionDao.insertSession(any()) }
    }
}
