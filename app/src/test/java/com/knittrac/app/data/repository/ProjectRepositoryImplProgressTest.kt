package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.FakeTimeProvider
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.ProjectDao
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.StitchCount
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.SQLException

class ProjectRepositoryImplProgressTest {

    private val projectDao: ProjectDao = mockk()
    private val timeProvider = FakeTimeProvider()
    private val repository = ProjectRepositoryImpl(projectDao, timeProvider)

    @Test
    fun updateProjectProgress_rows1_success() = runTest {
        coEvery { projectDao.updateProjectProgress(any(), any(), any(), any(), any(), any()) } returns 1

        val result = repository.updateProjectProgress(
            projectId = 1L,
            currentPatternRowId = PatternRowId.create(10L),
            currentRowNumber = RowCount.create(5),
            currentStitch = StitchCount.ZERO
        )

        assertTrue(result is Result.Success)
        coVerify { projectDao.updateProjectProgress(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun updateProjectProgress_rows0_notFoundError() = runTest {
        coEvery { projectDao.updateProjectProgress(any(), any(), any(), any(), any(), any()) } returns 0

        val result = repository.updateProjectProgress(
            projectId = 99L,
            currentPatternRowId = null,
            currentRowNumber = RowCount.ZERO,
            currentStitch = StitchCount.ZERO
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.NotFoundError)
    }

    @Test
    fun updateProjectProgress_sqlException_databaseError() = runTest {
        coEvery { projectDao.updateProjectProgress(any(), any(), any(), any(), any(), any()) } throws SQLException("boom")

        val result = repository.updateProjectProgress(
            projectId = 1L,
            currentPatternRowId = null,
            currentRowNumber = RowCount.ZERO,
            currentStitch = StitchCount.ZERO
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }
}
