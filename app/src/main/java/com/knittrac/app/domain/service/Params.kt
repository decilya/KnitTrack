package com.knittrac.app.domain.service

import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.ProjectId

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
    val projectId: ProjectId,
    val rowId: PatternRowId
)

/**
 * Параметры для отметки следующего ряда схемы как связанного.
 *
 * «Следующий» определяется в [ProgressService.markNextRow]: если у проекта
 * ещё нет текущего ряда — берётся первый ряд схемы, иначе — ряд с rowNumber
 * на единицу больше текущего.
 *
 * @property projectId Идентификатор проекта.
 */
data class MarkNextRowParams(
    val projectId: ProjectId
)
