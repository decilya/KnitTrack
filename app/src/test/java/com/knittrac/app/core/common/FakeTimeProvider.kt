package com.knittrac.app.core.common

/**
 * Фейковый провайдер времени для тестов.
 * Позволяет независимо управлять wall clock и monotonic временем.
 */
class FakeTimeProvider(
    var wallClock: Long = 1_000L,
    var monotonic: Long = 0L
) : TimeProvider {
    override fun currentTimeMillis(): Long = wallClock
    override fun elapsedRealtime(): Long = monotonic
}
