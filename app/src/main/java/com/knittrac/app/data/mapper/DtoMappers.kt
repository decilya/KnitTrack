package com.knittrac.app.data.mapper

import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.StitchCount
import com.knittrac.app.domain.entity.Session

/**
 * Мапперы Domain ↔ DTO для сериализации в JSON.
 *
 * Отдельный слой от Entity↔Domain мапперов:
 * - EntityMappers — Domain ↔ Room (структура таблиц),
 * - DtoMappers — Domain ↔ JSON (формат бэкапа).
 *
 * DTO знают про Domain, Domain не знает ни про Entity, ни про DTO.
 * Value Objects и enum разворачиваются в примитивы для стабильного JSON.
 */

/**
 * Project → ProjectDto. Category и syncStatus сериализуются через name,
 * чтобы JSON содержал стабильные имена enum.
 */
fun Project.toDto(): ProjectDto = ProjectDto(
    id = id,
    name = name.value,
    category = category.name,
    createdAt = createdAt,
    totalTimeSeconds = totalTimeSeconds.seconds,
    totalRows = totalRows.value,
    targetRowCount = targetRowCount?.value,
    patternId = patternId,
    currentRowNumber = currentRowNumber.value,
    currentPatternRowId = currentPatternRowId,
    currentStitch = currentStitch.value,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name
)

/**
 * ProjectDto → Project. Все поля парсятся безопасно с fallback:
 * неизвестная категория → OTHER, пустое имя → DEFAULT,
 * отрицательное время → ZERO, неизвестный syncStatus → PENDING.
 */
fun ProjectDto.toDomain(): Project = Project(
    id = id,
    name = ProjectName.createOrDefault(name),
    category = Category.fromNameOrDefault(category),
    createdAt = createdAt,
    totalTimeSeconds = Duration.createOrNull(totalTimeSeconds) ?: Duration.ZERO,
    totalRows = RowCount.createOrDefault(totalRows),
    targetRowCount = targetRowCount?.let { RowCount.createOrDefault(it) },
    patternId = patternId,
    currentRowNumber = RowCount.createOrDefault(currentRowNumber),
    currentPatternRowId = currentPatternRowId,
    currentStitch = StitchCount.createOrDefault(currentStitch),
    updatedAt = updatedAt,
    syncStatus = SyncStatus.fromNameOrDefault(syncStatus)
)

/**
 * Session → SessionDto. Duration и syncStatus разворачиваются в примитивы.
 */
fun Session.toDto(): SessionDto = SessionDto(
    id = id,
    projectId = projectId,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationSeconds = durationSeconds.seconds,
    rowCount = rowCount,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name
)

/**
 * SessionDto → Session. Длительность и статус — через безопасный fallback.
 */
fun SessionDto.toDomain(): Session = Session(
    id = id,
    projectId = projectId,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationSeconds = Duration.createOrNull(durationSeconds) ?: Duration.ZERO,
    rowCount = rowCount,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.fromNameOrDefault(syncStatus)
)
