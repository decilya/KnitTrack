package com.knittrac.app.domain.service
import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportDataServiceTest : BaseTest() {
    private val syncRepository: SyncRepository = mockk()
    private val service = ImportDataService(syncRepository, coroutineRule.testDispatcher)
    @Test
    fun `execute returns success on import`() = runTest {
        val params = ImportDataParams("{}")
        coEvery { syncRepository.importFromJson(params.json) } returns Result.Success(Unit)
        val result = service(params)
        assertTrue(result is Result.Success)
        coVerify { syncRepository.importFromJson(params.json) }
    }
}
