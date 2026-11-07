package com.knittrac.app.data.repository

import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.SessionId
import com.google.gson.Gson
import com.knittrac.app.core.common.FakeTimeProvider
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.repository.BackupRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class DataExporterImplTest {

    private val backupRepository = mockk<BackupRepository>()
    private val gson = Gson()
    private val timeProvider = FakeTimeProvider(wallClock = 123456789L)
    private val exporter = DataExporterImpl(backupRepository, gson, timeProvider, UnconfinedTestDispatcher())

    @Test
    fun exporttoWritesJsonVersion12WithProjectsSessionsAndPatternTables() =
        runTest(UnconfinedTestDispatcher()) {
        val project = Project(
            id = ProjectId.create(1L), 
            name = ProjectName.create("Project A"), 
            category = Category.KNITTING,
            createdAt = 100L, 
            updatedAt = 200L, 
            totalTimeSeconds = Duration.create(500L),
            syncStatus = SyncStatus.PENDING
        )
        val session = Session(
            id = SessionId.create(1L), 
            projectId = ProjectId.create(1L), 
            startTimestamp = 100L, 
            endTimestamp = 200L,
            durationSeconds = Duration.create(100L), 
            rowCount = RowCount.create(10), 
            updatedAt = 200L,
            syncStatus = SyncStatus.PENDING
        )

        val context = mockk<BackupRepository.ExportContext>()
        coEvery { context.forEachProject(any()) } coAnswers {
            val action = firstArg<suspend (Project) -> Unit>()
            action(project)
        }
        coEvery { context.forEachSession(any()) } coAnswers {
            val action = firstArg<suspend (Session) -> Unit>()
            action(session)
        }
        coEvery { context.forEachPattern(any()) } returns Unit
        coEvery { context.forEachPatternRow(any()) } returns Unit
        coEvery { context.forEachSymbol(any()) } returns Unit
        coEvery { context.forEachPatternRepeat(any()) } returns Unit
        coEvery { context.forEachPatternOperation(any()) } returns Unit
        coEvery { backupRepository.exportSnapshot<Unit>(any()) } coAnswers {
            val block = firstArg<suspend (BackupRepository.ExportContext) -> Unit>()
            block(context)
        }

        val outputStream = ByteArrayOutputStream()
        val result = exporter.exportTo(outputStream)

        assertTrue(result is Result.Success)
        val json = outputStream.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("\"version\":\"1.2\""))
        assertTrue(json.contains("\"exportDate\":123456789"))
        assertTrue(json.contains("\"name\":\"Project A\""))
        assertTrue(json.contains("\"rowCount\":10"))
        assertTrue(json.contains("\"patterns\":[]"))
        assertTrue(json.contains("\"patternRows\":[]"))
        assertTrue(json.contains("\"symbols\":[]"))
        assertTrue(json.contains("\"patternRepeats\":[]"))
        assertTrue(json.contains("\"patternOperations\":[]"))
        val order = listOf(
            "\"patterns\"",
            "\"patternRows\"",
            "\"symbols\"",
            "\"patternRepeats\"",
            "\"patternOperations\"",
            "\"projects\"",
            "\"sessions\""
        )
        val indexes = order.map { json.indexOf(it) }
        assertTrue(indexes.all { it >= 0 })
        assertEquals(indexes, indexes.sorted())
    }
}
