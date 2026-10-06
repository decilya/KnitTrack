package com.knittrac.app.domain.service

import com.knittrac.app.domain.entity.Category

/**
 * Параметры для добавления нового проекта.
 */
data class AddProjectParams(
    val name: String, 
    val category: Category
)

/**
 * Параметры для сохранения сессии вязания.
 * Используется в SaveSessionService для расчета длительности и создания Domain-модели.
 * 
 * @property projectId Идентификатор проекта.
 * @property startTimestamp Время начала сессии (в миллисекундах).
 * @property endTimestamp Время окончания сессии (в миллисекундах).
 * @property rowCount Количество связанных рядов.
 */
data class SaveSessionParams(
    val projectId: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val rowCount: Int
)

/**
 * Параметры для импорта данных из JSON.
 */
data class ImportDataParams(val json: String)
