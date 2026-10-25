package com.knittrac.app.domain.service

import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.PatternRowId

/**
 * Параметры для добавления нового проекта.
 */
data class AddProjectParams(
    val name: String,
    val category: Category,
    val targetRowCount: Int? = null
)

/**
 * Параметры для сохранения сессии вязания.
 */
data class SaveSessionParams(
    val projectId: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val rowCount: Int
)

/**
 * Параметры для отметки ряда как связанного.
 *
 * @property projectId Идентификатор проекта.
 * @property rowId Идентификатор ряда схемы (внутри привязанной к проекту схемы).
 */
data class MarkRowParams(
    val projectId: Long,
    val rowId: PatternRowId
)
