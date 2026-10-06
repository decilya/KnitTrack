package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [ExportDataService].
 * 
 * Проверяют корректное делегирование вызова в [SyncRepository]
 * и правильную обработку как успешных, так и ошибочных результатов.
 * ВАЖНО: Метод invoke() больше не принимает параметров.
 */
class ExportDataServiceTest {

    private val syncRepository: SyncRepository = mockk()
    private val service = ExportDataService(syncRepository)

    /**
     * Проверяет успешный сценарий: возврат JSON-строки при успешном экспорте.
     */
    @Test
    fun `invoke should return json on success`() = runTest {
        // Arrange
        val expectedJson = """{"version":"1.0"}"""
        coEvery { syncRepository.exportToJson() } returns Result.Success(expectedJson)

        // Act
        val result = service() // Вызов без параметров

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(expectedJson, (result as Result.Success).data)
        coVerify(exactly = 1) { syncRepository.exportToJson() }
    }

    /**
     * Проверяет сценарий ошибки: возврат Result.Error при сбое репозитория.
     */
    @Test
    fun `invoke should return error on failure`() = runTest {
        // Arrange
        coEvery { syncRepository.exportToJson() } returns Result.Error(mockk(relaxed = true))

        // Act
        val result = service()

        // Assert
        assertTrue(result is Result.Error)
    }
}
