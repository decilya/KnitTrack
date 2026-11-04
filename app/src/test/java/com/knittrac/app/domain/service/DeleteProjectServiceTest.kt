package com.knittrac.app.domain.service
import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
class DeleteProjectServiceTest : BaseTest() {
    private val projectRepository: ProjectRepository = mockk()
    private val service = DeleteProjectService(projectRepository, coroutineRule.testDispatcher)
    @Test
    fun `execute returns success on delete`() = runTest {
        coEvery { projectRepository.deleteProject(ProjectId.create(1L)) } returns Result.Success(Unit)
        val result = service(ProjectId.create(1L))
        assertTrue(result is Result.Success)
        coVerify { projectRepository.deleteProject(ProjectId.create(1L)) }
    }
}
