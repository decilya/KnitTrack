package com.knittrac.app.data.repository

import com.google.gson.Gson
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.BackupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class DataImporterImplTest {

    private val backupRepository = mockk<BackupRepository>(relaxed = true)
    private val gson = Gson()
    private val importer = DataImporterImpl(backupRepository, gson, UnconfinedTestDispatcher())

    private val testJson = """
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

    @Test
    fun `importFrom successfully parses JSON and inserts into DAOs`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { backupRepository.withTransaction<Unit>(any()) } coAnswers {
            val block = firstArg<suspend () -> Unit>()
            block()
        }

        val inputStream = ByteArrayInputStream(testJson.toByteArray(Charsets.UTF_8))
        val result = importer.importFrom(inputStream)

        assertTrue(result is Result.Success)
        coVerify { backupRepository.saveProjectsBatch(match { it.size == 1 && it[0].name.value == "Project A" }) }
        coVerify { backupRepository.saveSessionsBatch(match { it.size == 1 && it[0].rowCount == 10 }) }
    }

    @Test
    fun `importFrom returns IoError on invalid JSON`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { backupRepository.withTransaction<Unit>(any()) } coAnswers {
            val block = firstArg<suspend () -> Unit>()
            block()
        }

        val invalidJson = "not a json"
        val inputStream = ByteArrayInputStream(invalidJson.toByteArray(Charsets.UTF_8))
        val result = importer.importFrom(inputStream)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.IoError)
    }
}
