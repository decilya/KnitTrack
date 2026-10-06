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
 */
data class SaveSessionParams(
    val projectId: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val rowCount: Int
)
