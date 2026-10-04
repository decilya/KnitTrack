package com.knittrac.app.data.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.domain.entity.Session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionRepositoryImplTest {

    private lateinit var sessionDao: SessionDao
    private lateinit var repository: SessionRepositoryImpl

    @Before
    fun setUp() {
        sessionDao = mockk()
        repository = SessionRepositoryImpl(sessionDao)
    }

    @Test
    fun `addSession should set PENDING sync status`() = runTest {
        // Arrange
        val session = Session(
            id = 0,
            projectId = 1L,
            startTime = 1000L,
            endTime = 2000L,
            rowCount = 10,
            syncStatus = SyncStatus.SYNCED.name
        )
        
        coEvery { sessionDao.insertSession(any()) } returns 1L

        // Act
        val result = repository.addSession(session)

        // Assert
        assertTrue(result is Result.Success)
        coVerify {
            sessionDao.insertSession(match { s ->
                s.syncStatus == SyncStatus.PENDING.name
            })
        }
    }
}
