package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.common.FakeTimeProvider
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [ProjectRepositoryImpl].
 * Проверяют корректный маппинг Entity ↔ Domain, делегирование в DAO,
 * а также обработку ситуации "0 rows affected" при UPDATE-операциях.
 */
class ProjectRepositoryImplTest {

    private val projectDao: ProjectDao = mockk()
    private val timeProvider = FakeTimeProvider()
    private val repository = ProjectRepositoryImpl(projectDao, timeProvider)

    private val testEntity = ProjectEntity(
        id = 1L,
        name = "Тестовый проект",
        category = Category.KNITTING.name,
        createdAt = 1_000L,
        updatedAt = 2_000L,
        totalTimeSeconds = 500L,
        syncStatus = SyncStatus.PENDING.name
    )

    /**
     * Поток проектов корректно маппится из Entity в Domain.
     */
    @Test
    fun getAllProjectsFlow_mapsEntitiesToDomainModels() = runTest {
        every { projectDao.getAllProjectsFlow() } returns flowOf(listOf(testEntity))

        val result = repository.getAllProjectsFlow().first()

        assertEquals(1, result.size)
        assertEquals("Тестовый проект", result[0].name.value)
        assertEquals(Category.KNITTING, result[0].category)
    }

    /**
     * Разовый запрос списка проектов с маппингом.
     */
    @Test
    fun getAllProjects_returnsSuccessWithMappedList() = runTest {
        coEvery { projectDao.getAllProjects() } returns listOf(testEntity)

        val result = repository.getAllProjects()

        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("Тестовый проект", list[0].name.value)
        coVerify { projectDao.getAllProjects() }
    }

    /**
     * Добавление проекта маппит Domain в Entity и возвращает id.
     */
    @Test
    fun addProject_mapsDomainToEntityAndReturnsId() = runTest {
        coEvery { projectDao.insertProject(any()) } returns 42L
        val project = Project(
            id = 0L,
            name = ProjectName.create("Новый проект"),
            category = Category.CROCHET,
            createdAt = 1_000L,
            totalTimeSeconds = Duration.ZERO,
            updatedAt = 1_000L,
            syncStatus = SyncStatus.PENDING
        )

        val result = repository.addProject(project)

        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data)
        coVerify { projectDao.insertProject(any()) }
    }

    /**
     * getProjectById возвращает NotFoundError, если записи нет.
     */
    @Test
    fun getProjectById_returnsNotFoundErrorWhenMissing() = runTest {
        coEvery { projectDao.getProjectById(99L) } returns null

        val result = repository.getProjectById(99L)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    /**
     * updateProject возвращает NotFoundError, если DAO вернул 0 rows
     * (запись с таким id не существует).
     */
    @Test
    fun updateProject_returnsNotFoundErrorWhenDaoReturnsZeroRows() = runTest {
        coEvery { projectDao.updateProject(any()) } returns 0
        val project = Project(
            id = 99L,
            name = ProjectName.create("Несуществующий"),
            category = Category.KNITTING,
            createdAt = 1_000L,
            totalTimeSeconds = Duration.ZERO,
            updatedAt = 1_000L,
            syncStatus = SyncStatus.PENDING
        )

        val result = repository.updateProject(project)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    /**
     * updateProject возвращает Success, если DAO вернул 1 row.
     */
    @Test
    fun updateProject_returnsSuccessWhenDaoReturnsOneRow() = runTest {
        coEvery { projectDao.updateProject(any()) } returns 1
        val project = Project(
            id = 1L,
            name = ProjectName.create("Существующий"),
            category = Category.KNITTING,
            createdAt = 1_000L,
            totalTimeSeconds = Duration.ZERO,
            updatedAt = 1_000L,
            syncStatus = SyncStatus.PENDING
        )

        val result = repository.updateProject(project)

        assertTrue(result is Result.Success)
        // Репозиторий проставляет PENDING независимо от входной модели.
        coVerify {
            projectDao.updateProject(match { it.syncStatus == SyncStatus.PENDING.name })
        }
    }

    /**
     * updateTotalTime возвращает NotFoundError, если проекта с таким id нет.
     */
    @Test
    fun updateProjectTotals_returnsNotFoundErrorWhenDaoReturnsZeroRows() = runTest {
        coEvery { projectDao.incrementProjectTotals(any(), any(), any(), any()) } returns 0

        val result = repository.updateProjectTotals(
            projectId = 99L,
            duration = Duration.create(60L),
            rows = 0
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    /**
     * updateTotalTime возвращает Success при rows = 1.
     */
    @Test
    fun updateProjectTotals_returnsSuccessWhenDaoReturnsOneRow() = runTest {
        coEvery { projectDao.incrementProjectTotals(any(), any(), any(), any()) } returns 1

        val result = repository.updateProjectTotals(
            projectId = 1L,
            duration = Duration.create(60L),
            rows = 0
        )

        assertTrue(result is Result.Success)
    }

    /**
     * updateSyncStatus делегирует в DAO и возвращает Success при rows = 1.
     * Без этого метода проект навсегда остаётся в PENDING.
     */
    @Test
    fun updateSyncStatus_returnsSuccessWhenDaoReturnsOneRow() = runTest {
        coEvery { projectDao.updateSyncStatus(1L, SyncStatus.SYNCED.name) } returns 1

        val result = repository.updateSyncStatus(1L, SyncStatus.SYNCED)

        assertTrue(result is Result.Success)
        coVerify { projectDao.updateSyncStatus(1L, SyncStatus.SYNCED.name) }
    }

    /**
     * updateSyncStatus возвращает NotFoundError, если проекта нет.
     */
    @Test
    fun updateSyncStatus_returnsNotFoundErrorWhenDaoReturnsZeroRows() = runTest {
        coEvery { projectDao.updateSyncStatus(99L, any()) } returns 0

        val result = repository.updateSyncStatus(99L, SyncStatus.SYNCED)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    /**
     * deleteProject вызывает DAO ровно один раз (без предварительного get).
     */
    @Test
    fun deleteProject_callsDaoOnce() = runTest {
        coEvery { projectDao.deleteProjectById(1L) } returns 1

        val result = repository.deleteProject(1L)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { projectDao.deleteProjectById(1L) }
    }

    /**
     * deleteProject идемпотентен: если строки уже нет (rows = 0),
     * возвращается Success, а не NotFoundError. Защищает UI от гонки
     * при повторном нажатии «Удалить».
     */
    @Test
    fun deleteProject_isIdempotentWhenDaoReturnsZeroRows() = runTest {
        coEvery { projectDao.deleteProjectById(99L) } returns 0

        val result = repository.deleteProject(99L)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { projectDao.deleteProjectById(99L) }
    }
}
