package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProjectByIdServiceTest : BaseTest() {
    private val projectRepository: ProjectRepository = mockk()
    private val service = GetProjectByIdService(projectRepository, coroutineRule.testDispatcher)

    @Test
    fun `execute returns project when exists`() = runTest {
        // ИСПРАВЛЕНО: добавлено .name к SyncStatus.SYNCED
        val project = Project(1L, "Тест", Category.KNITTING, System.currentTimeMillis(), 0, System.currentTimeMillis(), SyncStatus.SYNCED.name)
        coEvery { projectRepository.getProjectById(1L) } returns Result.Success(project)
        val result = service(1L)
        assertTrue(result is Result.Success)
        assertEquals(project, (result as Result.Success).data)
        coVerify { projectRepository.getProjectById(1L) }
    }
}
