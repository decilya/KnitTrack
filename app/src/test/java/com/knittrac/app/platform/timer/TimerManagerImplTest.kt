package com.knittrac.app.platform.timer

import com.knittrac.app.core.common.TimeProvider
import com.knittrac.app.domain.service.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты для [TimerManagerImpl].
 *
 * Особенность работы с бесконечным таймером:
 *
 * [TimerManagerImpl.start] запускает корутину вида
 * while (isActive) { ...; delay(1000L) } — бесконечный цикл,
 * который никогда не завершается сам по себе.
 *
 * Это создаёт две проблемы в тестах:
 *
 * 1. Нельзя использовать advanceUntilIdle(): он выполняет задачи
 *    в scheduler'е, пока они есть. После каждого тика таймер снова
 *    планирует delay(1000L), поэтому advanceUntilIdle() крутит
 *    виртуальное время бесконечно и не возвращается.
 *
 * 2. Нельзя оставлять job активным в конце теста: runTest дожидается
 *    завершения всех корутин в scheduler'е, а бесконечный цикл не
 *    завершится никогда.
 *
 * Решение — использовать advanceTimeBy(1000L) (выполняет задачи только в текущей
 * виртуальной точке) и обязательно вызывать [TimerManagerImpl.reset]
 * в finally, чтобы отменить job до выхода из runTest.
 *
 * Зачем FakeTimeProvider:
 *
 * Прямое использование System.currentTimeMillis() и
 * SystemClock.elapsedRealtime() делает тесты недетерминированными:
 * нельзя эмулировать NTP-сдвиг, перевод часов или смену часового
 * пояса. [FakeTimeProvider] позволяет управлять обоими часами вручную.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerImplTest {

    private lateinit var timerManager: TimerManagerImpl
    private lateinit var fakeTime: FakeTimeProvider
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        fakeTime = FakeTimeProvider()
        timerManager = TimerManagerImpl(testDispatcher, fakeTime)
    }

    /**
     * Обёртка вокруг [runTest] с гарантированной отменой таймера.
     *
     * Без finally { timerManager.reset() } бесконечный job таймера
     * останется в scheduler'е после завершения тела теста, и runTest
     * будет ждать его вечно, пока не сработает таймаут.
     */
    private fun runTimerTest(body: suspend TestScope.() -> Unit) =
        runTest(testDispatcher) {
            try {
                body()
            } finally {
                timerManager.reset()
            }
        }

    // Старт

    /**
     * При первом start() sessionStartTime берётся из wall clock.
     * Это значение уйдёт в Session.startTimestamp при сохранении.
     */
    @Test
    fun `start sets sessionStartTime from wall clock and switches state to RUNNING`() =
        runTimerTest {
            timerManager.start()

            assertEquals(
                FakeTimeProvider.INITIAL_WALL_MILLIS,
                timerManager.sessionStartTime
            )
            assertEquals(TimerState.RUNNING, timerManager.state.value)
        }

    /**
     * Повторный start() после pause() не должен менять sessionStartTime.
     * Это момент первой сессии, он остаётся в БД неизменным,
     * чтобы статистика не сбивалась.
     */
    @Test
    fun `resume after pause does not change sessionStartTime`() = runTimerTest {
        timerManager.start()
        val firstStartTime = timerManager.sessionStartTime

        timerManager.pause()

        // Прошло 60 секунд реального времени (и wall, и monotonic)
        fakeTime.wallMillis += 60_000L
        fakeTime.elapsedMillis += 60_000L

        timerManager.start()
        val secondStartTime = timerManager.sessionStartTime

        assertEquals(firstStartTime, secondStartTime)
    }

    // Расчёт elapsedTime

    /**
     * После 5 секунд monotonic-времени elapsedTime = 5.
     *
     * advanceTimeBy(1000L) выполняет только задачи в текущей виртуальной точке —
     * один тик таймера, который читает актуальное
     * [FakeTimeProvider.elapsedMillis] и обновляет _elapsedTime.
     * В отличие от advanceUntilIdle(), возвращается сразу, не крутя
     * виртуальное время.
     */
    @Test
    fun `elapsedTime reflects monotonic time after tick`() = runTimerTest {
        timerManager.start()

        // Прокручиваем мок-часы на 5 секунд
        fakeTime.elapsedMillis += 5_000L
        // Выполняем один тик — корутина читает новое время
        advanceTimeBy(1000L)

        assertEquals(5L, timerManager.elapsedTime.value)
    }

    /**
     * NTP-сценарий: wall clock идёт назад на 10 минут (синхронизация
     * после сбоя часов), но monotonic-время продолжает идти вперёд.
     * elapsedTime должен расти, а не падать.
     *
     * Это ключевая проверка того, что System.currentTimeMillis()
     * в таймере не используется — иначе тест упал бы.
     */
    @Test
    fun `NTP shift backwards does not affect elapsedTime`() = runTimerTest {
        timerManager.start()

        // 5 секунд monotonic — elapsedTime = 5
        fakeTime.elapsedMillis += 5_000L
        advanceTimeBy(1000L)
        assertEquals(5L, timerManager.elapsedTime.value)

        // NTP-синхронизация: wall clock прыгает назад на 10 минут
        fakeTime.wallMillis -= 600_000L

        // Monotonic двигаем ещё на 5 секунд
        fakeTime.elapsedMillis += 5_000L
        advanceTimeBy(1000L)

        // elapsedTime вырос до 10, несмотря на сдвиг wall clock
        assertEquals(10L, timerManager.elapsedTime.value)

        // sessionStartTime — по-прежнему начальное значение
        assertEquals(
            FakeTimeProvider.INITIAL_WALL_MILLIS,
            timerManager.sessionStartTime
        )
    }

    // Пауза

    /**
     * pause() останавливает корутину, но не сбрасывает elapsedTime.
     * Пользователь может нажать «Пауза», выпить чаю, нажать «Старт» —
     * и время продолжит накапливаться с того же места.
     */
    @Test
    fun `pause preserves elapsedTime and switches state to PAUSED`() =
        runTimerTest {
            timerManager.start()

            fakeTime.elapsedMillis += 5_000L
            advanceTimeBy(1000L)
            val elapsedBeforePause = timerManager.elapsedTime.value

            timerManager.pause()

            assertEquals(TimerState.PAUSED, timerManager.state.value)
            assertEquals(elapsedBeforePause, timerManager.elapsedTime.value)
        }

    /**
     * После pause() дальнейший рост monotonic-времени не влияет
     * на elapsedTime: корутина отменена, тики не выполняются.
     */
    @Test
    fun `elapsedTime does not grow during pause`() = runTimerTest {
        timerManager.start()

        fakeTime.elapsedMillis += 5_000L
        advanceTimeBy(1000L)
        val beforePause = timerManager.elapsedTime.value

        timerManager.pause()

        // Прошло 30 секунд, но таймер на паузе
        fakeTime.elapsedMillis += 30_000L
        advanceTimeBy(1000L)

        assertEquals(beforePause, timerManager.elapsedTime.value)
    }

    /**
     * После resume (start после pause) elapsedTime продолжает расти,
     * исключая время паузы.
     *
     * Сценарий:
     * - 5 секунд работы (elapsedTime = 5)
     * - pause на 30 секунд
     * - resume, ещё 3 секунды работы
     * - итог: elapsedTime = 8, а не 38
     */
    @Test
    fun `elapsedTime excludes paused duration on resume`() = runTimerTest {
        timerManager.start()
        fakeTime.elapsedMillis += 5_000L
        advanceTimeBy(1000L)

        timerManager.pause()

        // На паузе прошло 30 секунд (и wall, и monotonic двигаются)
        fakeTime.wallMillis += 30_000L
        fakeTime.elapsedMillis += 30_000L

        timerManager.start()

        // Ещё 3 секунды работы
        fakeTime.elapsedMillis += 3_000L
        advanceTimeBy(1000L)

        // Ожидаем 8, а не 38 — 30 секунд паузы не считаются
        assertEquals(8L, timerManager.elapsedTime.value)
    }

    // Reset

    /**
     * reset() возвращает всё в исходное состояние: ни sessionStartTime,
     * ни elapsedTime, ни накопленные паузы не сохраняются.
     */
    @Test
    fun `reset clears all state`() = runTimerTest {
        timerManager.start()
        fakeTime.elapsedMillis += 10_000L
        advanceTimeBy(1000L)
        timerManager.pause()

        timerManager.reset()

        assertEquals(0L, timerManager.sessionStartTime)
        assertEquals(0L, timerManager.elapsedTime.value)
        assertEquals(TimerState.IDLE, timerManager.state.value)
    }
}

/**
 * Управляемый провайдер времени для тестов.
 *
 * Зачем два независимых поля:
 *
 * [TimerManagerImpl] использует два разных типа времени:
 * - [wallMillis] — аналог System.currentTimeMillis() для sessionStartTime.
 * - [elapsedMillis] — аналог SystemClock.elapsedRealtime() для elapsedTime.
 *
 * Тесты могут двигать их независимо: например, эмулировать NTP-сдвиг
 * (wall назад, monotonic вперёд) — так проверяется устойчивость
 * к переводу часов.
 *
 * Значения по умолчанию:
 *
 * [INITIAL_WALL_MILLIS] — произвольное реалистичное значение (близко
 * к 2001 году), чтобы отличать «до старта» от «после старта».
 * elapsedMillis стартует с 0 — момент загрузки устройства
 * в тестовой симуляции.
 */
private class FakeTimeProvider : TimeProvider {

    /** Мок wall clock (Unix epoch). Управляется тестом. */
    var wallMillis: Long = INITIAL_WALL_MILLIS

    /** Мок monotonic-времени (с момента загрузки). Управляется тестом. */
    var elapsedMillis: Long = 0L

    override fun currentTimeMillis(): Long = wallMillis

    override fun elapsedRealtime(): Long = elapsedMillis

    companion object {
        /** 2001-09-09T01:46:40Z — просто для отличия от 0. */
        const val INITIAL_WALL_MILLIS = 1_000_000_000_000L
    }
}
