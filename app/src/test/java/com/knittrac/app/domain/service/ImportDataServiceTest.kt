package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [ImportDataService].
 * 
 * Проверяют, что UseCase принимает объект [ImportDataParams] и корректно
 * извлекает из него JSON-строку для передачи в [SyncRepository].
 */
class ImportDataServiceTest {

    private val syncRepository: SyncRepository = mockk()
    private val service = ImportDataService(syncRepository)

    /**
     * Проверяет, что метод invoke корректно вызывает importFromJson с JSON из параметров.
     */
    @Test
    fun `invoke should call importFromJson with given json`() = runTest {
        // Arrange
        val json = """{"version":"1.0"}"""
        val params = ImportDataParams(json)
        coEvery { syncRepository.importFromJson(json) } returns Result.Success(Unit)

        // Act
        val result = service(params) // Передаем объект параметров

        // Assert
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { syncRepository.importFromJson(json) }
    }
}
