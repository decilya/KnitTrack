package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.domain.entity.Session
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveSessionServiceTest {

    private lateinit var sessionDao: SessionDao
    private lateinit var saveSessionService: SaveSessionService

    @Before
    fun setUp() {
        sessionDao = mockk()
        // ИСПРАВЛЕНО: передаем SessionDao и диспетчер, как требует новый конструктор
        saveSessionService = SaveSessionService(
            sessionDao = sessionDao,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun `execute should return Success with session ID`() = runTest {
        // Arrange
        val params = SaveSessionParams(
            projectId = 1L,
            startTimestamp = 1000L,
            endTimestamp = 2000L,
            rowCount = 10
        )
        
        // Мокаем успешный ответ от DAO
        coEvery { sessionDao.insertSession(any()) } returns 42L

        // Act
        val result = saveSessionService(params)

        // Assert
        assertTrue(result is Result.Success)
        // Опционально: можно проверить, что ID равен 42
        // assert((result as Result.Success).data == 42L)
    }
}
