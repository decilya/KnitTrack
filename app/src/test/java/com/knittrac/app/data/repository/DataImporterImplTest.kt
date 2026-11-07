package com.knittrac.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.google.gson.GsonBuilder
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.repository.BackupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.ByteArrayInputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DataImporterImplTest {

    private val backupRepository = mockk<BackupRepository>(relaxed = true)
    private val gson = GsonBuilder().create()
    private val importer = DataImporterImpl(backupRepository, gson, UnconfinedTestDispatcher())

    private val testJson = """
        {
          "version": "1.0",
          "exportDate": 123456789,
          "projects": [
            {
              "id": 1, "name": "Project A", "category": "KNITTING",
              "createdAt": 100, "totalTimeSeconds": 500,
              "patternId": 7, "currentPatternRowId": 9, "currentRowNumber": 4,
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
    fun importFromSuccessfullyParsesJSONAndInsertsIntoDAOs() = runTest(UnconfinedTestDispatcher()) {
        coEvery { backupRepository.withTransaction<Unit>(any()) } coAnswers {
            val block = firstArg<suspend () -> Unit>()
            block()
        }

        val inputStream = ByteArrayInputStream(testJson.toByteArray(Charsets.UTF_8))
        val result = importer.importFrom(inputStream)

        assertTrue(result is Result.Success)
        coVerify {
            backupRepository.saveProjectsBatch(
                match {
                    it.size == 1 &&
                        it[0].name.value == "Project A" &&
                        it[0].patternId == null &&
                        it[0].currentPatternRowId == null &&
                        it[0].currentRowNumber.value == 0
                }
            )
        }
        coVerify { backupRepository.saveSessionsBatch(match { it.size == 1 && it[0].rowCount == RowCount.create(10) }) }
    }

    @Test
    fun importFromReturnsIoErrorOnInvalidJSON() = runTest(UnconfinedTestDispatcher()) {
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

    /**
     * P2 (LIN/BIZ): Проверяет, что при импорте бэкапа с битым repeatCode
     * (orphan) возвращается ValidationError, а не DatabaseError.
     */
    @Test
    fun importFrom_returnsValidationError_onOrphanRepeatCode() = runTest {
        val backupRepo = mockk<BackupRepository>()
        
        coEvery { backupRepo.withTransaction<Unit>(any()) } throws 
            SQLiteConstraintException("Раппорт не принадлежит схеме")

        val importer = DataImporterImpl(backupRepo, GsonBuilder().create(), UnconfinedTestDispatcher())
        
        val json = """
            {
              "version": "1.2",
              "patternOperations": [
                { "patternRowId": 1, "columnStart": 0, "symbolCode": "K", "repeatCode": "ORPHAN" }
              ]
            }
        """.trimIndent()

        val result = importer.importFrom(json.byteInputStream())
        
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.ValidationError)
        assertTrue(error.message.contains("несуществующими раппортами"))
    }
}
