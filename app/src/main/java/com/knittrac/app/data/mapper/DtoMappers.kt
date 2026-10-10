package com.knittrac.app.data.mapper

import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session
import timber.log.Timber

/**
 * Мапперы Domain ↔ DTO для сериализации в JSON.
 *
 * Отдельный слой от Entity↔Domain мапперов:
 * - EntityMappers — Domain ↔ Room (структура таблиц),
 * - DtoMappers — Domain ↔ JSON (формат бэкапа).
 *
 * DTO знают про Domain, Domain не знает ни про Entity, ни про DTO.
 */

/**
 * Project → ProjectDto. Category сериализуется через name,
 * чтобы JSON содержал стабильное имя enum ("KNITTING", ...).
 */
fun Project.toDto(): ProjectDto = ProjectDto(
    id = id,
    name = name,
    category = category.name,
    createdAt = createdAt,
    totalTimeSeconds = totalTimeSeconds,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * ProjectDto → Project. Категория парсится безопасно:
 * неизвестное значение (старый бэкап, повреждённый JSON) логируется
 * через Timber.w и превращается в Category.OTHER, а не крашит импорт.
 */
fun ProjectDto.toDomain(): Project = Project(
    id = id,
    name = name,
    category = categoryFromString(category),
    createdAt = createdAt,
    totalTimeSeconds = totalTimeSeconds,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * Session → SessionDto.
 */
fun Session.toDto(): SessionDto = SessionDto(
    id = id,
    projectId = projectId,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationSeconds = durationSeconds,
    rowCount = rowCount,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * SessionDto → Session.
 */
fun SessionDto.toDomain(): Session = Session(
    id = id,
    projectId = projectId,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationSeconds = durationSeconds,
    rowCount = rowCount,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * Безопасный парсинг имени категории.
 *
 * Не использует Category.valueOf — тот бросает IllegalArgumentException
 * при неизвестном значении. Fallback на Category.OTHER сохраняет
 * пользовательские данные (сессия/проект импортируются, просто
 * категория отображается как «Другое»).
 */
private fun categoryFromString(value: String): Category =
    Category.entries.firstOrNull { it.name == value } ?: run {
        Timber.w("Unknown category in DTO: '%s', falling back to OTHER", value)
        Category.OTHER
    }
