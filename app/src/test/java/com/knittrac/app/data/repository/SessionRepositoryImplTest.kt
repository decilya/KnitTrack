package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.common.FakeTimeProvider
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.Duration
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
 * Проверяют маппинг Entity → Domain, вызов атомарных методов DAO
 * и обработку "0 rows affected" при UPDATE-операциях.
 */
class SessionRepositoryImplTest {

    private val sessionDao: SessionDao = mockk()
    private val timeProvider = FakeTimeProvider()
    private val repository = SessionRepositoryImpl(sessionDao, timeProvider)

    private val testEntity = SessionEntity(
        id = 1L, projectId = 10L, startTimestamp = 1_000L, endTimestamp = 61_000L,
        durationSeconds = 60L, rowCount = 5, updatedAt = 62_000L,
        syncStatus = SyncStatus.PENDING.name
    )

    /**
     * Атомарное добавление сессии маппит Domain в Entity и вызывает
     * транзакционный метод DAO с правильными параметрами.
     */
    @Test
    fun addSessionAtomically_callsDaoTransactionMethodAndReturnsId() = runTest {
        coEvery {
            sessionDao.insertSessionAndIncrementProjectTotals(any(), any(), any())
        } returns 100L

        val session = Session(
            projectId = 10L, startTimestamp = 1_000L, endTimestamp = 61_000L,
            durationSeconds = Duration.create(60L), rowCount = 5, updatedAt = 1_000L
        )

        val result = repository.addSessionAtomically(session)

        assertTrue(result is Result.Success)
        assertEquals(100L, (result as Result.Success).data)
        coVerify {
            sessionDao.insertSessionAndIncrementProjectTotals(any(), 60L, any())
        }
    }

    /**
     * updateSession возвращает NotFoundError, если DAO вернул 0 rows
     * (сессии с таким id нет — вставки не происходит).
     */
    @Test
    fun updateSession_returnsNotFoundErrorWhenDaoReturnsZeroRows() = runTest {
        coEvery { sessionDao.updateSession(any()) } returns 0
        val session = Session(
            id = 99L,
            projectId = 1L,
            startTimestamp = 1_000L,
            endTimestamp = 61_000L,
            durationSeconds = Duration.create(60L),
            rowCount = 5,
            updatedAt = 1_000L
        )

        val result = repository.updateSession(session)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    /**
     * updateSession возвращает Success при rows = 1.
     */
    @Test
    fun updateSession_returnsSuccessWhenDaoReturnsOneRow() = runTest {
        coEvery { sessionDao.updateSession(any()) } returns 1
        val session = Session(
            id = 1L,
            projectId = 1L,
            startTimestamp = 1_000L,
            endTimestamp = 61_000L,
            durationSeconds = Duration.create(60L),
            rowCount = 5,
            updatedAt = 1_000L
        )

        val result = repository.updateSession(session)

        assertTrue(result is Result.Success)
        // Репозиторий проставляет PENDING независимо от входной модели.
        coVerify {
            sessionDao.updateSession(match { it.syncStatus == SyncStatus.PENDING.name })
        }
    }

    /**
     * updateSyncStatus возвращает NotFoundError, если сессии с таким id нет.
     */
    @Test
    fun updateSyncStatus_returnsNotFoundErrorWhenDaoReturnsZeroRows() = runTest {
        coEvery { sessionDao.updateSyncStatus(any(), any()) } returns 0

        val result = repository.updateSyncStatus(
            sessionId = 99L,
            status = SyncStatus.SYNCED
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    /**
     * updateSyncStatus возвращает Success при rows = 1.
     */
    @Test
    fun updateSyncStatus_returnsSuccessWhenDaoReturnsOneRow() = runTest {
        coEvery { sessionDao.updateSyncStatus(any(), any()) } returns 1

        val result = repository.updateSyncStatus(
            sessionId = 1L,
            status = SyncStatus.SYNCED
        )

        assertTrue(result is Result.Success)
    }

    /**
     * deleteSessionAndDecrementProjectTotals возвращает NotFoundError,
     * если DAO бросает NoSuchElementException (сессия не найдена).
     */
    @Test
    fun deleteSessionAndDecrementProjectTotals_returnsNotFoundErrorWhenDaoThrowsNoSuchElement() = runTest {
        coEvery {
            sessionDao.deleteSessionAndDecrementProjectTotals(any(), any())
        } throws NoSuchElementException("Сессия id=999 не найдена")

        val result = repository.deleteSessionAndDecrementProjectTotals(999L)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }
}
