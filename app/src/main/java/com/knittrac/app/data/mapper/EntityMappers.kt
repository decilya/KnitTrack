package com.knittrac.app.data.mapper

import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.Session

/**
 * Маппер [ProjectEntity] (Data слой) в [Project] (Domain слой).
 *
 * Категория преобразуется из String в Enum.
 * Название и общее время проходят через Value Objects с безопасным fallback:
 * невалидные данные из БД (пустое имя, отрицательное время) не крашат UI.
 */
fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = ProjectName.createOrDefault(name),
    category = Category.valueOf(category),
    createdAt = createdAt,
    totalTimeSeconds = Duration.createOrNull(totalTimeSeconds) ?: Duration.ZERO,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * Маппер [Project] (Domain слой) в [ProjectEntity] (Data слой).
 * Value Objects разворачиваются в примитивы для хранения в SQLite.
 */
fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name.value,
    category = category.name,
    createdAt = createdAt,
    totalTimeSeconds = totalTimeSeconds.seconds,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * Маппер [SessionEntity] (Data слой) в [Session] (Domain слой).
 * ВАЖНО: имена полей (startTimestamp, endTimestamp) строго совпадают
 * в обеих моделях. Длительность проходит через [Duration] с fallback
 * на [Duration.ZERO].
 */
fun SessionEntity.toDomain(): Session = Session(
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
 * Маппер [Session] (Domain слой) в [SessionEntity] (Data слой).
 */
fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    projectId = projectId,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationSeconds = durationSeconds.seconds,
    rowCount = rowCount,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)
