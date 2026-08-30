package com.knittrac.app.domain.service
import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class ExportDataServiceTest : BaseTest() {
    private val syncRepository: SyncRepository = mockk()
    private val service = ExportDataService(syncRepository, coroutineRule.testDispatcher)
    @Test
    fun `execute returns json on success`() = runTest {
        coEvery { syncRepository.exportToJson() } returns Result.Success("{}")
        val result = service(Unit)
        assertTrue(result is Result.Success)
        assertEquals("{}", (result as Result.Success).data)
        coVerify { syncRepository.exportToJson() }
    }
}
