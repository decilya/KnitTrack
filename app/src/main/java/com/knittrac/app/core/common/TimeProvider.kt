package com.knittrac.app.core.common

/**
 * Абстракция для получения текущего времени.
 *
 * Внедрение этого интерфейса вместо прямого вызова [System.currentTimeMillis]
 * и [android.os.SystemClock.elapsedRealtime] позволяет легко тестировать
 * компоненты, зависящие от времени, подменяя реализацию (DIP + testability).
 *
 * Разделение двух методов:
 * - [currentTimeMillis] — wall clock (Unix epoch). Для сохранения в БД
 *   (startTimestamp, updatedAt). Может идти назад при NTP-синхронизации.
 * - [elapsedRealtime] — монотонное время (мс с загрузки устройства).
 *   Для расчёта длительности сессий. Никогда не идёт назад.
 */
interface TimeProvider {
    /** Wall clock: миллисекунды с начала Unix-эпохи. */
    fun currentTimeMillis(): Long

    /** Monotonic: миллисекунды с момента загрузки устройства, включая сон. */
    fun elapsedRealtime(): Long
}
