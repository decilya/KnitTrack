package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.DataExporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Юнит-тесты для [ExportDataService].
 * Проверяют делегирование в [DataExporter] без дополнительной логики.
 */
class ExportDataServiceTest {

    private val dataExporter: DataExporter = mockk()
    private val service = ExportDataService(dataExporter)

    @Test
    fun `invoke delegates to DataExporter and returns success`() = runTest {
        val stream = ByteArrayOutputStream()
        coEvery { dataExporter.exportTo(stream) } returns Result.Success(Unit)

        val result = service(stream)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dataExporter.exportTo(stream) }
    }

    @Test
    fun `invoke returns error on exporter failure`() = runTest {
        val stream = ByteArrayOutputStream()
        coEvery { dataExporter.exportTo(stream) } returns Result.Error(mockk(relaxed = true))

        val result = service(stream)

        assertTrue(result is Result.Error)
        coVerify(exactly = 1) { dataExporter.exportTo(stream) }
    }
}
