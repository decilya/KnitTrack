package com.knittrac.app.domain.service
import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class AddProjectServiceTest : BaseTest() {
    private val projectRepository: ProjectRepository = mockk()
    private val service = AddProjectService(projectRepository, coroutineRule.testDispatcher)
    @Test
    fun `execute returns success with project id`() = runTest {
        coEvery { projectRepository.addProject(any()) } returns Result.Success(42L)
        val result = service(AddProjectParams("Шарф", Category.KNITTING))
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data)
        coVerify { projectRepository.addProject(any()) }
    }
}
