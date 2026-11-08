package com.knittrac.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.knittrac.app.core.common.FakeTimeProvider
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.domain.entity.ProjectId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Round-trip 3NF-проекции: `current_row_number` derived из `pattern_rows.row_number`
 * через LEFT JOIN (P2, v10).
 *
 * `UnconfinedTestDispatcher`: без `advanceUntilIdle`, операции БД синхронные
 * (`allowMainThreadQueries`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProjectRepositoryImplRoomTest {

    private fun openDb(context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(AppDatabase.DATABASE_CALLBACK)
            .build()

    private suspend fun seedPatternWithRows(db: AppDatabase) {
        db.patternDao().insertPattern(
            PatternEntity(
                id = 1L,
                name = "Схема",
                technique = "KNITTING",
                direction = "LTR",
                isReversible = false
            )
        )
        db.patternDao().insertRows(
            listOf(
                PatternRowEntity(id = 10L, patternId = 1L, rowNumber = 5),
                PatternRowEntity(id = 11L, patternId = 1L, rowNumber = 7)
            )
        )
    }

    private fun projectEntity(
        id: Long = 0L,
        patternId: Long? = 1L,
        currentPatternRowId: Long? = 10L,
        currentStitch: Int = 0
    ): ProjectEntity = ProjectEntity(
        id = id,
        name = "P$id",
        category = "KNITTING",
        createdAt = 0L,
        updatedAt = 0L,
        totalTimeSeconds = 0L,
        patternId = patternId,
        currentPatternRowId = currentPatternRowId,
        currentStitch = currentStitch,
        syncStatus = "PENDING"
    )

    /**
     * getProjectById: currentPatternRowId = 10, у которого row_number = 5,
     * derived currentRowNumber = 5 (через LEFT JOIN).
     */
    @Test
    fun getProjectById_derivesCurrentRowNumberFromJoin() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openDb(context)
        try {
            seedPatternWithRows(db)
            val newId = db.projectDao().insertProject(projectEntity(currentPatternRowId = 10L))
            val repository = ProjectRepositoryImpl(db.projectDao(), FakeTimeProvider())

            val result = repository.getProjectById(ProjectId.create(newId))

            assertTrue("expected Success, got $result", result is Result.Success)
            val project = (result as Result.Success).data
            assertEquals(10L, project.currentPatternRowId?.value)
            assertEquals(5, project.currentRowNumber.value)
        } finally {
            db.close()
        }
    }

    /**
     * getProjectById: currentPatternRowId = null → derived currentRowNumber = 0.
     */
    @Test
    fun getProjectById_nullRowId_returnsZero() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openDb(context)
        try {
            seedPatternWithRows(db)
            val newId = db.projectDao().insertProject(projectEntity(currentPatternRowId = null))
            val repository = ProjectRepositoryImpl(db.projectDao(), FakeTimeProvider())

            val result = repository.getProjectById(ProjectId.create(newId))

            assertTrue(result is Result.Success)
            val project = (result as Result.Success).data
            assertEquals(null, project.currentPatternRowId)
            assertEquals(0, project.currentRowNumber.value)
        } finally {
            db.close()
        }
    }

    /**
     * getAllProjectsFlow: 2 проекта (с row и без) — каждый получает свой derived номер.
     */
    @Test
    fun getAllProjectsFlow_joinDerivesForEach() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openDb(context)
        try {
            seedPatternWithRows(db)
            val id1 = db.projectDao().insertProject(projectEntity(currentPatternRowId = 10L))
            val id2 = db.projectDao().insertProject(projectEntity(currentPatternRowId = 11L))
            val id3 = db.projectDao().insertProject(projectEntity(currentPatternRowId = null))
            val repository = ProjectRepositoryImpl(db.projectDao(), FakeTimeProvider())

            val projects = repository.getAllProjectsFlow().first()

            assertEquals(3, projects.size)
            assertEquals(5, projects.first { it.id.value == id1 }.currentRowNumber.value)
            assertEquals(7, projects.first { it.id.value == id2 }.currentRowNumber.value)
            assertEquals(0, projects.first { it.id.value == id3 }.currentRowNumber.value)
        } finally {
            db.close()
        }
    }

    /**
     * Ключ к D&D: при изменении `pattern_rows.row_number` Flow
     * `getAllProjectsFlow` переизлучается (Room инвалидирует JOIN-запрос).
     *
     * `runBlocking` (не `runTest`): Room invalidation tracker работает в
     * реальных потоках ArchTaskExecutor, виртуальное время `runTest` его не
     * двигает. `delay(100)` — время на подписку коллектора.
     */
    @Test
    fun getAllProjectsFlow_reemitsOnPatternRowUpdate() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openDb(context)
        try {
            seedPatternWithRows(db)
            val id = db.projectDao().insertProject(projectEntity(currentPatternRowId = 10L))
            val repository = ProjectRepositoryImpl(db.projectDao(), FakeTimeProvider())

            val emissions = async {
                repository.getAllProjectsFlow()
                    .map { list -> list.firstOrNull { it.id.value == id }?.currentRowNumber?.value }
                    .filterNotNull()
                    .take(2)
                    .toList()
            }

            delay(100L) // дать коллектору подписаться и получить initial emission (5)

            db.patternDao().updateRowNumber(10L, 9)

            val received = withTimeoutOrNull(5_000L) { emissions.await() }
            assertEquals(listOf(5, 9), received)
        } finally {
            db.close()
        }
    }
}
