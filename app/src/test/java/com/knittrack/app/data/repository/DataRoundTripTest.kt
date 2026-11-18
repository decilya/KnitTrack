package com.knittrack.app.data.repository

import com.knittrack.app.data.local.DatabaseCallback

import com.knittrack.app.domain.entity.ProjectId
import com.knittrack.app.domain.entity.SessionId
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.knittrack.app.data.parser.GsonBackupParser
import com.knittrack.app.core.common.FakeTimeProvider
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.common.SyncStatus
import com.knittrack.app.data.local.AppDatabase
import com.knittrack.app.data.mapper.toDomain
import com.knittrack.app.data.mapper.toEntity
import com.knittrack.app.domain.entity.Category
import com.knittrack.app.domain.entity.ColumnStart
import com.knittrack.app.domain.entity.Duration
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.entity.PatternDirection
import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.PatternName
import com.knittrack.app.domain.entity.PatternOperation
import com.knittrack.app.domain.entity.PatternRepeat
import com.knittrack.app.domain.entity.PatternRow
import com.knittrack.app.domain.entity.PatternRowId
import com.knittrack.app.domain.entity.Project
import com.knittrack.app.domain.entity.ProjectName
import com.knittrack.app.domain.entity.RowCount
import com.knittrack.app.domain.entity.RowNumber
import com.knittrack.app.domain.entity.Session
import com.knittrack.app.domain.entity.StitchCount
import com.knittrack.app.domain.entity.Symbol
import com.knittrack.app.domain.entity.SymbolCategory
import com.knittrack.app.domain.entity.Technique
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    /**
     * Сессия, созданная локально после экспорта, выживает после импорта.
     *
     * Регрессия: batch @Insert(REPLACE) в saveSessionsBatch делал
     * DELETE+INSERT на session_id, а DELETE на sessions не запускал
     * CASCADE для других сессий; но REPLACE на projects (saveProjectsBatch)
     * через FK CASCADE удалял ВСЕ сессии проекта, включая локальные.
     * После UPDATE-then-INSERT локальные сессии целы.
     */
    @Test
    fun importOverExistingData_preservesSessionsCreatedAfterExport() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dest = openDb(context)
        try {
            seed(dest)
            val bytes = ByteArrayOutputStream()
            assertTrue(exporter(dest).exportTo(bytes) is Result.Success)

            dest.sessionDao().insertSession(
                session().copy(id = SessionId.create(99L), startTimestamp = 500L).toEntity()
            )

            assertTrue(
                importer(dest).importFrom(ByteArrayInputStream(bytes.toByteArray())) is Result.Success
            )

            val sessions = dest.sessionDao().getAllSessions().map { it.id }.toSet()
            assertEquals(setOf(1L, 99L), sessions)
        } finally {
            dest.close()
        }
    }

    /**
     * Локальный проект, отсутствующий в бэкапе, сохраняет привязку к схеме
     * и текущий ряд.
     *
     * Регрессия: REPLACE на patterns (savePatternsBatch) через FK CASCADE
     * удалял pattern_rows, а через SET NULL обнулял projects.pattern_id и
     * projects.current_pattern_row_id у ВСЕХ проектов, ссылающихся на
     * перезаписанную схему, включая проекты, которых нет в бэкапе.
     * После UPDATE-then-INSERT этих потерь нет.
     */
    @Test
    fun importOverExistingData_preservesLocalProjectAbsentFromBackup() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = openDb(context)
        val dest = openDb(context)
        try {
            seed(source)
            val bytes = ByteArrayOutputStream()
            assertTrue(exporter(source).exportTo(bytes) is Result.Success)

            seed(dest)
            dest.projectDao().insertProject(
                linkedProject().copy(id = ProjectId.create(99L)).toEntity()
            )

            assertTrue(
                importer(dest).importFrom(ByteArrayInputStream(bytes.toByteArray())) is Result.Success
            )

            val localProject = dest.projectDao().getProjectById(99L)?.toDomain()
            assertNotNull(localProject)
            assertEquals(PatternId.create(1L), localProject?.patternId)
            assertEquals(PatternRowId.create(10L), localProject?.currentPatternRowId)
        } finally {
            source.close()
            dest.close()
        }
    }

    /**
     * Импорт частичного бэкапа succeeds, даже если локальная схема
     * использует символ, которого нет в бэкапе.
     *
     * Регрессия: REPLACE на symbols (saveSymbolsBatch) удалял системный
     * символ K, а pattern_operations.symbol_code -> symbols ON DELETE
     * RESTRICT отказывал, если K использовался в локальной схеме,
     * отсутствующей в бэкапе. SQLiteConstraintException -> откат
     * всего импорта с ValidationError. После UPDATE-then-INSERT символ
     * K обновляется без DELETE, RESTRICT не срабатывает.
     */
    @Test
    fun importOverExistingData_partialBackupWithLocalPattern() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = openDb(context)
        val dest = openDb(context)
        try {
            seed(source)
            val bytes = ByteArrayOutputStream()
            assertTrue(exporter(source).exportTo(bytes) is Result.Success)

            seed(dest)
            val localPattern = Pattern(
                id = PatternId.create(77L),
                name = PatternName.create("Локальная"),
                technique = Technique.KNITTING,
                direction = PatternDirection.LTR,
                isReversible = false
            )
            dest.patternDao().insertPattern(localPattern.toEntity())
            val localRow = PatternRow(
                id = PatternRowId.create(770L),
                rowNumber = RowNumber.create(1)
            )
            dest.patternDao().insertRows(listOf(localRow.toEntity(77L)))
            val localOp = PatternOperation(
                patternRowId = PatternRowId.create(770L),
                columnStart = ColumnStart.create(0),
                symbol = knitSymbol(),
                repeatCode = null
            )
            dest.patternDao().insertOperations(listOf(localOp.toEntity()))

            val result = importer(dest).importFrom(ByteArrayInputStream(bytes.toByteArray()))
            assertTrue("Expected Success, got $result", result is Result.Success)

            val localOps = dest.patternDao().getOperationsForRow(770L)
            assertEquals(1, localOps.size)
            assertEquals("K", localOps[0].symbolCode)
        } finally {
            source.close()
            dest.close()
        }
    }

    /**
     * Двойной импорт одного файла даёт то же состояние.
     *
     * Страж: до фикса REPLACE тоже был идемпотентен (DELETE+INSERT
     * возвращает то же состояние), но с потерей локальных данных.
     * Тест фиксирует контракт: повторный импорт не должен накапливать
     * дубликаты или терять строки.
     */
    @Test
    fun importOverExistingData_isIdempotent() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dest = openDb(context)
        try {
            seed(dest)
            val bytes = ByteArrayOutputStream()
            assertTrue(exporter(dest).exportTo(bytes) is Result.Success)
            assertTrue(
                importer(dest).importFrom(ByteArrayInputStream(bytes.toByteArray())) is Result.Success
            )

            val sessions1 = dest.sessionDao().getAllSessions().map { it.id }.toSet()
            val projects1 = dest.projectDao().getAllProjects().size
            val patterns1 = dest.patternDao().getAllPatterns().size
            val rows1 = dest.patternDao().getAllRows().size
            val ops1 = dest.patternDao().getAllOperations().size
            val repeats1 = dest.patternRepeatDao().getAllRepeats().size

            assertTrue(
                importer(dest).importFrom(ByteArrayInputStream(bytes.toByteArray())) is Result.Success
            )

            assertEquals(sessions1, dest.sessionDao().getAllSessions().map { it.id }.toSet())
            assertEquals(projects1, dest.projectDao().getAllProjects().size)
            assertEquals(patterns1, dest.patternDao().getAllPatterns().size)
            assertEquals(rows1, dest.patternDao().getAllRows().size)
            assertEquals(ops1, dest.patternDao().getAllOperations().size)
            assertEquals(repeats1, dest.patternRepeatDao().getAllRepeats().size)
        } finally {
            dest.close()
        }
    }

    /**
     * После импорта инвариант projects.total_* == SUM(sessions)
     * восстановлен.
     *
     * Регрессия: seed-фикстура linkedProject() имеет totals=0, а
     * session() — duration=100s, rowCount=10. Без пересчёта totals
     * остались бы 0/0 после импорта — расхождение с суммой сессий.
     * recomputeProjectTotals() обновляет totals на 100/10.
     */
    @Test
    fun importOverExistingData_recomputesTotals() = runTest(UnconfinedTestDispatcher()) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = openDb(context)
        val dest = openDb(context)
        try {
            seed(source)
            val bytes = ByteArrayOutputStream()
            assertTrue(exporter(source).exportTo(bytes) is Result.Success)

            assertTrue(
                importer(dest).importFrom(ByteArrayInputStream(bytes.toByteArray())) is Result.Success
            )

            val project = dest.projectDao().getProjectById(1L)?.toDomain()
            assertNotNull(project)
            assertEquals(Duration.create(100L), project?.totalTimeSeconds)
            assertEquals(RowCount.create(10), project?.totalRows)
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
        GsonBackupParser(Gson()),
        FakeTimeProvider(wallClock = 1L),
        UnconfinedTestDispatcher()
    )

    private fun importer(db: AppDatabase) = DataImporterImpl(
        BackupRepositoryImpl(db, db.projectDao(), db.sessionDao()),
        GsonBackupParser(Gson()),
        UnconfinedTestDispatcher()
    )

    private suspend fun seed(db: AppDatabase) {
        val symbol = knitSymbol()
        db.symbolDao().insertSymbols(listOf(symbol.toEntity()))
        db.patternDao().insertPattern(scheme().toEntity())
        db.patternDao().insertRows(listOf(firstRow().toEntity(1L)))
        db.patternRepeatDao().insertRepeats(listOf(repeatA().toEntity(1L)))
        db.patternDao().insertOperations(listOf(operation(symbol).toEntity()))
        db.projectDao().insertProject(linkedProject().toEntity())
        db.sessionDao().insertSession(session().toEntity())
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
