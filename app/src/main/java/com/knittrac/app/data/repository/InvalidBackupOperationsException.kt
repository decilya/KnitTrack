package com.knittrac.app.data.repository

/**
 * Сигнатура нарушения целостности БД при импорте операций.
 *
 * Возникает, когда в бэкапе есть операции с несуществующими:
 * - `pattern_row_id` (нарушение FK);
 * - `symbol_code` (нарушение FK);
 * - `repeat_code` (нарушение триггера `trg_repeat_code_check_insert`).
 *
 * Вынесено в отдельный тип, чтобы Application-слой ([DataImporterImpl])
 * не зависел от `SQLiteConstraintException` и конкретного текста ошибки.
 *
 * @param cause Исходное исключение БД.
 */
class InvalidBackupOperationsException(cause: Throwable) : Exception(cause)
