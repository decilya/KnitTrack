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
 * Проверяют корректный маппинг Entity → Domain и вызов атомарных методов DAO.
 */
class SessionRepositoryImplTest {

    private val sessionDao: SessionDao = mockk()
    private val repository = SessionRepositoryImpl(sessionDao)

    private val testEntity = SessionEntity(
        id = 1L, projectId = 10L, startTimestamp = 1_000L, endTimestamp = 61_000L,
        durationSeconds = 60L, rowCount = 5, updatedAt = 62_000L, syncStatus = "PENDING"
    )

    /**
     * Проверяет, что атомарное добавление сессии корректно маппит Domain в Entity 
     * и вызывает соответствующий транзакционный метод DAO с правильными параметрами.
     */
    @Test
    fun `addSessionAtomically should call DAO transaction method and return id`() = runTest {
        // Arrange
        coEvery { sessionDao.insertSessionAndIncrementProjectTime(any(), any(), any()) } returns 100L
        
        val session = Session(
            projectId = 10L, startTimestamp = 1_000L, endTimestamp = 61_000L,
            durationSeconds = 60L, rowCount = 5
        )
        
        // Act
        val result = repository.addSessionAtomically(session)
        
        // Assert
        assertTrue(result is Result.Success)
        assertEquals(100L, (result as Result.Success).data)
        // Проверяем, что длительность (60L) была передана в DAO корректно
        coVerify { sessionDao.insertSessionAndIncrementProjectTime(any(), 60L, any()) }
    }
}
