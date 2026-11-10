package com.knittrac.app.data.repository

import com.knittrac.app.data.local.DatabaseCallback

import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.SessionId
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.knittrac.app.core.common.FakeTimeProvider
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.AppDatabase
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.data.mapper.toEntity
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRepeat
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Session
import com.knittrac.app.domain.entity.StitchCount
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.entity.Technique
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DataRoundTripTest {

    @Test
    fun exportThenImport_restoresPatternTablesAndProjectLink() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = openDb(context)
        val dest = openDb(context)
        try {
            seed(source)
            val bytes = ByteArrayOutputStream()
            val exported = exporter(source).exportTo(bytes)
            assertTrue(exported is Result.Success)
            val imported = importer(dest).importFrom(ByteArrayInputStream(bytes.toByteArray()))
            assertTrue(imported is Result.Success)

            val pattern = dest.patternDao().getAllPatterns().single()
            assertEquals(1L, pattern.id)
            assertEquals("Схема", pattern.name)
            val row = dest.patternDao().getAllRows().single()
            assertEquals(10L, row.id)
            assertEquals(1L, row.patternId)
            assertEquals(1, row.rowNumber)
            assertEquals(
                setOf("K", "P", "O", "K2tog", "SSK", "SL"),
                dest.symbolDao().getAllSymbols().map { it.code }.toSet()
            )
            val repeat = dest.patternRepeatDao().getAllRepeats().single()
            assertEquals(1L, repeat.patternId)
            assertEquals("A", repeat.code)
            val operation = dest.patternDao().getAllOperations().single()
            assertEquals(10L, operation.patternRowId)
            assertEquals("K", operation.symbolCode)
            assertEquals("A", operation.repeatCode)
            val project = dest.projectDao().getAllProjects().single().toDomain()
            assertEquals(1L, project.patternId?.value)
            assertEquals(10L, project.currentPatternRowId?.value)
            assertEquals(1, project.currentRowNumber.value)
            assertEquals(1L, dest.sessionDao().getAllSessions().single().projectId)
        } finally {
            source.close()
            dest.close()
        }
    }

    private fun openDb(context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(DatabaseCallback)
            .build()

    private fun exporter(db: AppDatabase) = DataExporterImpl(
        BackupRepositoryImpl(db, db.projectDao(), db.sessionDao()),
        Gson(),
        FakeTimeProvider(wallClock = 1L),
        UnconfinedTestDispatcher()
    )

    private fun importer(db: AppDatabase) = DataImporterImpl(
        BackupRepositoryImpl(db, db.projectDao(), db.sessionDao()),
        Gson(),
        UnconfinedTestDispatcher()
    )

    private suspend fun seed(db: AppDatabase) {
        val symbol = knitSymbol()
        db.symbolDao().insertSymbols(listOf(symbol.toEntity()))
        db.patternDao().insertPattern(scheme().toEntity())
        db.patternDao().insertRows(listOf(firstRow().toEntity(1L)))
        db.patternRepeatDao().insertRepeats(listOf(repeatA().toEntity(1L)))
        db.patternDao().insertOperations(listOf(operation(symbol).toEntity()))
        db.projectDao().insertProjects(listOf(linkedProject().toEntity()))
        db.sessionDao().insertSessions(listOf(session().toEntity()))
    }

    private fun knitSymbol() = Symbol(
        code = "K",
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD
    )

    private fun scheme() = Pattern(
        id = PatternId.create(1L),
        name = PatternName.create("Схема"),
        technique = Technique.KNITTING,
        direction = PatternDirection.LTR,
        isReversible = false
    )

    private fun firstRow() = PatternRow(
        id = PatternRowId.create(10L),
        rowNumber = RowNumber.create(1)
    )

    private fun repeatA() = PatternRepeat(
        code = "A",
        stitchCount = StitchCount.create(2),
        rowCount = RowCount.create(1)
    )

    private fun operation(symbol: Symbol) = PatternOperation(
        patternRowId = PatternRowId.create(10L),
        columnStart = ColumnStart.create(0),
        symbol = symbol,
        repeatCode = "A"
    )

    private fun linkedProject() = Project(
        id = ProjectId.create(1L),
        name = ProjectName.create("Проект"),
        category = Category.KNITTING,
        createdAt = 100L,
        patternId = PatternId.create(1L),
        currentRowNumber = RowCount.create(1),
        currentPatternRowId = PatternRowId.create(10L),
        updatedAt = 200L,
        syncStatus = SyncStatus.PENDING
    )

    private fun session() = Session(
        id = SessionId.create(1L),
        projectId = ProjectId.create(1L),
        startTimestamp = 100L,
        endTimestamp = 200L,
        durationSeconds = Duration.create(100L),
        rowCount = RowCount.create(10),
        updatedAt = 200L,
        syncStatus = SyncStatus.PENDING
    )
}
