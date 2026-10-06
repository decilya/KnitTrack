package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.DataImporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Юнит-тесты для [ImportDataService].
 * Проверяют делегирование в [DataImporter].
 */
class ImportDataServiceTest {

    private val dataImporter: DataImporter = mockk()
    private val service = ImportDataService(dataImporter)

    @Test
    fun `invoke delegates to DataImporter and returns success`() = runTest {
        val stream = ByteArrayInputStream("{}".toByteArray())
        coEvery { dataImporter.importFrom(stream) } returns Result.Success(Unit)

        val result = service(stream)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dataImporter.importFrom(stream) }
    }

    @Test
    fun `invoke returns error on importer failure`() = runTest {
        val stream = ByteArrayInputStream("{}".toByteArray())
        coEvery { dataImporter.importFrom(stream) } returns Result.Error(mockk(relaxed = true))

        val result = service(stream)

        assertTrue(result is Result.Error)
        coVerify(exactly = 1) { dataImporter.importFrom(stream) }
    }
}
