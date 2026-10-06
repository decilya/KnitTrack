package com.knittrac.app.data.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
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
 * Проверяют корректный маппинг Entity ↔ Domain и делегирование вызовов в DAO.
 */
class ProjectRepositoryImplTest {

    private val projectDao: ProjectDao = mockk()
    private val repository = ProjectRepositoryImpl(projectDao)

    private val testEntity = ProjectEntity(
        id = 1L,
        name = "Тестовый проект",
        category = Category.KNITTING.name,
        createdAt = 1_000L,
        updatedAt = 2_000L,
        totalTimeSeconds = 500L,
        syncStatus = "PENDING"
    )

    /**
     * Проверяет, что поток проектов корректно маппится из Entity в Domain-модель.
     */
    @Test
    fun `getAllProjectsFlow should map entities to domain models`() = runTest {
        // Arrange
        every { projectDao.getAllProjectsFlow() } returns flowOf(listOf(testEntity))

        // Act
        val result = repository.getAllProjectsFlow().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals("Тестовый проект", result[0].name)
        assertEquals(Category.KNITTING, result[0].category)
    }

    /**
     * Проверяет успешное получение списка проектов с маппингом.
     */
    @Test
    fun `getAllProjects should return success with mapped list`() = runTest {
        // Arrange
        coEvery { projectDao.getAllProjects() } returns listOf(testEntity)

        // Act
        val result = repository.getAllProjects()

        // Assert
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("Тестовый проект", list[0].name)
        coVerify { projectDao.getAllProjects() }
    }

    /**
     * Проверяет, что добавление проекта корректно маппит Domain в Entity и возвращает ID.
     */
    @Test
    fun `addProject should map domain to entity and return id`() = runTest {
        // Arrange
        coEvery { projectDao.insertProject(any()) } returns 42L
        val project = Project(
            id = 0L,
            name = "Новый проект",
            category = Category.CROCHET,
            createdAt = 1_000L,
            totalTimeSeconds = 0L,
            updatedAt = 1_000L,
            syncStatus = "PENDING"
        )

        // Act
        val result = repository.addProject(project)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data)
        coVerify { projectDao.insertProject(any()) }
    }

    /**
     * Проверяет, что запрос несуществующего проекта возвращает ошибку.
     */
    @Test
    fun `getProjectById should return error when not found`() = runTest {
        // Arrange
        coEvery { projectDao.getProjectById(99L) } returns null

        // Act
        val result = repository.getProjectById(99L)

        // Assert
        assertTrue(result is Result.Error)
    }
}
