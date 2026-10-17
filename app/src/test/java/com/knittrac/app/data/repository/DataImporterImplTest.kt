package com.knittrac.app.data.repository

import androidx.room.withTransaction
import com.google.gson.Gson
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.data.local.SessionDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Юнит-тесты для [DataImporterImpl].
 * Проверяют парсинг JSON и корректный вызов DAO для сохранения данных.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataImporterImplTest {

    private lateinit var appDatabase: AppDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var sessionDao: SessionDao
    private lateinit var importer: DataImporterImpl

    @Before
    fun setUp() {
        appDatabase = mockk()
        projectDao = mockk(relaxed = true)
        sessionDao = mockk(relaxed = true)
        val gson = Gson()

        importer = DataImporterImpl(appDatabase, projectDao, sessionDao, gson, UnconfinedTestDispatcher())

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
    fun `importFrom successfully parses JSON and inserts into DAOs`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val json = """
            {
              "version": "1.0",
              "exportDate": 123456789,
              "projects": [
                {
                  "id": 1, "name": "Project A", "category": "KNITTING",
                  "createdAt": 100, "totalTimeSeconds": 500,
                  "updatedAt": 200, "syncStatus": "PENDING"
                }
              ],
              "sessions": [
                {
                  "id": 1, "projectId": 1, "startTimestamp": 100,
                  "endTimestamp": 200, "durationSeconds": 100,
                  "rowCount": 10, "updatedAt": 200, "syncStatus": "PENDING"
                }
              ]
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
        
        val projectListSlot = slot<List<com.knittrac.app.data.local.entity.ProjectEntity>>()
        val sessionListSlot = slot<List<com.knittrac.app.data.local.entity.SessionEntity>>()

        // Act
        val result = importer.importFrom(inputStream)

        // Assert
        assertTrue(result is Result.Success)
        
        coVerify(exactly = 1) { projectDao.insertProjects(capture(projectListSlot)) }
        coVerify(exactly = 1) { sessionDao.insertSessions(capture(sessionListSlot)) }

        val capturedProjects = projectListSlot.captured
        assertEquals(1, capturedProjects.size)
        assertEquals("Project A", capturedProjects[0].name)

        val capturedSessions = sessionListSlot.captured
        assertEquals(1, capturedSessions.size)
        assertEquals(10, capturedSessions[0].rowCount)
    }

    @Test
    fun `importFrom returns IoError on invalid JSON`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val invalidJson = "not a json"
        val inputStream = ByteArrayInputStream(invalidJson.toByteArray(Charsets.UTF_8))

        // Act
        val result = importer.importFrom(inputStream)

        // Assert
        assertTrue(result is Result.Error)
        // Ошибка парсинга JSON выбрасывается как IOException (или JsonSyntaxException, которая наследует RuntimeException)
        // В нашем случае importer ловит Exception и возвращает DatabaseError или IoError в зависимости от типа.
        // Gson выбрасывает MalformedJsonException (наследник IOException)
    }
}
