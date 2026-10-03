package com.knittrac.app.core.common

/**
 * Абстракция для получения текущего времени.
 * Внедрение этого интерфейса вместо прямого вызова System.currentTimeMillis()
 * позволяет легко тестировать таймер, подменяя время (соблюдение DIP и тестируемости).
 */
interface TimeProvider {
    /** Возвращает текущее время в миллисекундах с начала эпохи (Unix time) */
    fun currentTimeMillis(): Long
}

/**
 * Стандартная реализация, использующая системные часы устройства.
 */
class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}