package com.knittrac.app.domain.service

import com.knittrac.app.domain.entity.Category

/**
 * Параметры для добавления нового проекта.
 *
 * Используется как входной параметр для [AddProjectService].
 *
 * @param name Название проекта (например, "Зимний шарф").
 * @param category Категория проекта (определяет тип рукоделия).
 *
 * @see AddProjectService Сервис для добавления проекта.
 */
data class AddProjectParams(
    val name: String,
    val category: Category
)

/**
 * Параметры для сохранения завершенной сессии работы.
 *
 * Используется как входной параметр для [SaveSessionService].
 *
 * @param projectId Идентификатор проекта, над которым велась работа.
 * @param startTimestamp Временная метка начала сессии (в миллисекундах).
 * @param endTimestamp Временная метка окончания сессии (в миллисекундах).
 * @param rowCount Количество выполненных рядов (или другая метрика).
 *
 * @see SaveSessionService Сервис для сохранения сессии.
 */
data class SaveSessionParams(
    val projectId: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val rowCount: Int
)

/**
 * Параметры для импорта данных из JSON.
 *
 * Используется как входной параметр для [ImportDataService].
 *
 * @param json JSON строка с данными для импорта.
 *
 * @see ImportDataService Сервис для импорта данных.
 */
data class ImportDataParams(
    val json: String
)