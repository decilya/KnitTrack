package com.knittrac.app.data.mapper

import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session

/**
 * Маппер для преобразования [ProjectEntity] (Data слой) в [Project] (Domain слой).
 * Категория преобразуется из String (хранится в БД) в Enum.
 */
fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    category = Category.valueOf(category),
    createdAt = createdAt,
    totalTimeSeconds = totalTimeSeconds,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * Маппер для преобразования [Project] (Domain слой) в [ProjectEntity] (Data слой).
 * Категория преобразуется из Enum в String для сохранения в БД.
 */
fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    category = category.name,
    createdAt = createdAt,
    totalTimeSeconds = totalTimeSeconds,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)

/**
 * Маппер для преобразования [SessionEntity] (Data слой) в [Session] (Domain слой).
 * ВАЖНО: имена полей (startTimestamp, endTimestamp) строго совпадают в обеих моделях.
 */
fun SessionEntity.toDomain(): Session = Session(
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
 * Маппер для преобразования [Session] (Domain слой) в [SessionEntity] (Data слой).
 */
fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    projectId = projectId,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationSeconds = durationSeconds,
    rowCount = rowCount,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)
