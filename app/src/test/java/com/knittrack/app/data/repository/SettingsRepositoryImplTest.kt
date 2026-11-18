package com.knittrack.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.preferences.SettingsKeys
import com.knittrack.app.domain.model.TimerSnapshot
import com.knittrack.app.domain.service.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

/**
 * Тесты персистентности [TimerSnapshot] в [SettingsRepositoryImpl].
 *
 * DataStore Preferences работает на JVM — Robolectric не нужен.
 *
 * **Ключевой момент:** DataStore создаётся в [TestScope.backgroundScope],
 * а не в `coroutineContext` теста. Если передать `coroutineContext`,
 * `runTest` считает DataStore-корутины «своими» и либо ждёт их вечно
 * (UncompletedCoroutinesError), либо закрывает файл до flush
 * (IOException в `SingleProcessDataStore.writeData`). `backgroundScope`
 * отменяется вместе с тестом, но `runTest` не ждёт его завершения —
 * DataStore успевает дописать файл до проверки.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    /**
     * Создаёт DataStore в [TestScope.backgroundScope] и репозиторий поверх
     * него, выполняет [block], после чего дожидается flush через
     * `dataStore.data.first()` (гарантия, что `edit` применён).
     *
     * Один вызов — один тест, один временный файл.
     */
    private suspend fun TestScope.withRepository(
        block: suspend (SettingsRepositoryImpl, DataStore<Preferences>) -> Unit
    ) {
        val file = File(tmpDir.root, "test_${UUID.randomUUID()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file }
        )
        block(SettingsRepositoryImpl(dataStore), dataStore)
        // Ждём, пока DataStore применит все накопленные edit (flush).
        dataStore.data.first()
    }

    /** Собирает тестовый снимок с разумными умолчаниями. */
    private fun snapshot(
        state: TimerState = TimerState.RUNNING,
        sessionStartTime: Long = 1_000L,
        sessionStartElapsed: Long = 500L,
        totalPausedDuration: Long = 0L,
        lastPauseElapsed: Long = 0L
    ) = TimerSnapshot(
        state = state,
        sessionStartTime = sessionStartTime,
        sessionStartElapsed = sessionStartElapsed,
        totalPausedDuration = totalPausedDuration,
        lastPauseElapsed = lastPauseElapsed
    )

    /** Пустой DataStore — flow эмитит null. */
    @Test
    fun timerSnapshotFlow_isNullWhenNoData() = runTest {
        withRepository { repository, _ ->
            assertNull(repository.timerSnapshotFlow.first())
        }
    }

    /** save → flow эмитит сохранённый снимок. */
    @Test
    fun saveTimerSnapshot_emitsSnapshotOnFlow() = runTest {
        withRepository { repository, dataStore ->
            val s = snapshot()
            val result = repository.saveTimerSnapshot(s)
            assertTrue(result is Result.Success)
            dataStore.data.first() // flush save перед чтением flow
            assertEquals(s, repository.timerSnapshotFlow.first())
        }
    }

    /** clear → flow снова null. */
    @Test
    fun clearTimerSnapshot_flowBecomesNull() = runTest {
        withRepository { repository, dataStore ->
            repository.saveTimerSnapshot(snapshot())
            dataStore.data.first() // flush save
            assertTrue(repository.timerSnapshotFlow.first() != null)
            val result = repository.clearTimerSnapshot()
            assertTrue(result is Result.Success)
            dataStore.data.first() // flush clear
            assertNull(repository.timerSnapshotFlow.first())
        }
    }

    /** Повреждённый state (неизвестное имя) — flow null, не крэш. */
    @Test
    fun timerSnapshotFlow_isNullWhenStateCorrupted() = runTest {
        withRepository { repository, dataStore ->
            dataStore.edit { it[SettingsKeys.TIMER_STATE] = "BROKEN" }
            dataStore.edit { it[SettingsKeys.TIMER_SESSION_START_TIME] = 1_000L }
            dataStore.data.first() // flush оба edit перед чтением flow
            assertNull(repository.timerSnapshotFlow.first())
        }
    }
}
