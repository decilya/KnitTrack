package com.knittrac.app.core.common

/**
 * Фейковый провайдер времени для тестов.
 * Позволяет независимо управлять wall clock и monotonic временем.
 *
 * Поля помечены @Volatile, чтобы изменения были видимы между потоками теста
 * (например, при запуске нескольких тестов на разных диспетчерах).
 */
class FakeTimeProvider(
    @Volatile var wallClock: Long = 1_000L,
    @Volatile var monotonic: Long = 0L
) : TimeProvider {
    override fun currentTimeMillis(): Long = wallClock
    override fun elapsedRealtime(): Long = monotonic
}
