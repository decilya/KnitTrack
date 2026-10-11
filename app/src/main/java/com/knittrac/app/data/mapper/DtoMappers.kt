package com.knittrac.app.data.mapper

import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
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
 * Value Objects разворачиваются в примитивы для стабильного JSON-формата:
 * name → String, durationSeconds → Long.
 */

/**
 * Project → ProjectDto. Category сериализуется через name,
 * чтобы JSON содержал стабильное имя enum ("KNITTING", ...).
 */
fun Project.toDto(): ProjectDto = ProjectDto(
    id = id,
    name = name.value,
    category = category.name,
    createdAt = createdAt,
    totalTimeSeconds = totalTimeSeconds.seconds,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * ProjectDto → Project. Категория парсится безопасно:
 * неизвестное значение (старый бэкап, повреждённый JSON) логируется
 * через Timber.w и превращается в Category.OTHER, а не крашит импорт.
 * Имя и totalTimeSeconds проходят через VO с fallback на дефолт.
 */
fun ProjectDto.toDomain(): Project = Project(
    id = id,
    name = ProjectName.createOrDefault(name),
    category = categoryFromString(category),
    createdAt = createdAt,
    totalTimeSeconds = Duration.createOrNull(totalTimeSeconds) ?: Duration.ZERO,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * Session → SessionDto. Duration разворачивается в Long-секунды.
 */
fun Session.toDto(): SessionDto = SessionDto(
    id = id,
    projectId = projectId,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationSeconds = durationSeconds.seconds,
    rowCount = rowCount,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * SessionDto → Session. Длительность проходит через [Duration]
 * с fallback на [Duration.ZERO] — битые бэкапы не крашат импорт.
 */
fun SessionDto.toDomain(): Session = Session(
    id = id,
    projectId = projectId,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationSeconds = Duration.createOrNull(durationSeconds) ?: Duration.ZERO,
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
