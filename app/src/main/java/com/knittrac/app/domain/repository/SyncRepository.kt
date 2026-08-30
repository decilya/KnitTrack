package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result

/**
 * Интерфейс репозитория для экспорта и импорта данных (синхронизация).
 *
 * Этот интерфейс определяет контракт для операций экспорта всех данных
 * приложения в JSON и импорта из JSON. Используется для резервного
 * копирования и восстановления данных.
 *
 * Реализация находится в data слое (com.knittrac.app.data.repository.SyncRepositoryImpl).
 *
 * @see com.knittrac.app.data.repository.SyncRepositoryImpl Реализация репозитория.
 */
interface SyncRepository {
    /**
     * Экспортирует все данные приложения в формат JSON.
     *
     * Собирает все проекты и сессии из базы данных и сериализует их
     * в JSON строку с использованием Gson. Формат:
     * ```json
     * {
     *   "projects": [...],
     *   "sessions": [...]
     * }
     * ```
     *
     * @return [Result] с JSON строкой, содержащей все данные,
     *         или [Result.Error] при ошибке.
     */
    suspend fun exportToJson(): Result<String>

    /**
     * Импортирует данные приложения из JSON строки.
     *
     * Десериализует JSON и вставляет данные в базу данных.
     * На Этапе 1 реализована базовая заглушка без разрешения конфликтов.
     *
     * @param json JSON строка с данными для импорта.
     * @return [Result.Success] при успешном импорте или [Result.Error] при ошибке.
     */
    suspend fun importFromJson(json: String): Result<Unit>
}