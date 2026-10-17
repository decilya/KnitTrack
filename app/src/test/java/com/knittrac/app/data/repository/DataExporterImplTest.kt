package com.knittrac.app.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.knittrac.app.core.common.FakeTimeProvider
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.data.local.entity.SessionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Юнит-тесты для [DataExporterImpl].
 * Проверяют корректность сериализации в JSON и обработку ошибок.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataExporterImplTest {

    private lateinit var projectDao: ProjectDao
    private lateinit var sessionDao: SessionDao
    private lateinit var appDatabase: AppDatabase
    private lateinit var deps: DataExportDependencies
    private lateinit var exporter: DataExporterImpl

    @Before
    fun setUp() {
        projectDao = mockk()
        sessionDao = mockk()
        appDatabase = mockk()
        
        val gson = Gson()
        val timeProvider = FakeTimeProvider(wallClock = 123456789L)

        deps = DataExportDependencies(projectDao, sessionDao, gson, appDatabase, timeProvider)
        exporter = DataExporterImpl(deps, UnconfinedTestDispatcher())

        // ВАЖНО: мокаем статический extension-метод Room
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { 
            appDatabase.withTransaction(any<suspend () -> Any?>()) 
        } coAnswers { 
            secondArg<suspend () -> Any?>().invoke() 
        }
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `exportTo successfully writes JSON with projects and sessions`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val projectEntity = ProjectEntity(
            id = 1L, name = "Project A", category = "KNITTING", 
            createdAt = 100L, updatedAt = 200L, totalTimeSeconds = 500L, syncStatus = "PENDING"
        )
        val sessionEntity = SessionEntity(
            id = 1L, projectId = 1L, startTimestamp = 100L, endTimestamp = 200L,
            durationSeconds = 100L, rowCount = 10, updatedAt = 200L, syncStatus = "PENDING"
        )
        
        coEvery { projectDao.getAllProjects() } returns listOf(projectEntity)
        coEvery { sessionDao.getAllSessions() } returns listOf(sessionEntity)

        val outputStream = ByteArrayOutputStream()

        // Act
        val result = exporter.exportTo(outputStream)

        // Assert
        assertTrue(result is Result.Success)
        
        val json = outputStream.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("\"version\":\"1.0\""))
        assertTrue(json.contains("\"exportDate\":123456789"))
        assertTrue(json.contains("\"name\":\"Project A\""))
        assertTrue(json.contains("\"rowCount\":10"))
        
        coVerify(exactly = 1) { projectDao.getAllProjects() }
        coVerify(exactly = 1) { sessionDao.getAllSessions() }
    }
}
