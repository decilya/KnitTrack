package com.knittrac.app.domain.entity

import androidx.annotation.Keep
import com.knittrac.app.core.common.SyncStatus

/**
 * Доменная модель сессии вязания.
 *
 * VO-миграция (P1-D, 02.11.2026): `id: SessionId`, `projectId: ProjectId`,
 * `rowCount: RowCount`. На границе с Room/DTO разворачиваются в примитивы
 * (в [SessionRepositoryImpl] и мапперах).
 *
 * @property id Уникальный идентификатор сессии.
 * @property projectId Идентификатор проекта-владельца.
 * @property startTimestamp Время начала сессии (в миллисекундах).
 * @property endTimestamp Время окончания сессии (в миллисекундах).
 * @property durationSeconds Длительность сессии (VO [Duration]).
 * @property rowCount Количество связанных рядов за сессию.
 * @property updatedAt Временная метка последнего обновления сессии.
 * @property syncStatus Статус синхронизации (по умолчанию PENDING).
 */
@Keep
data class Session(
    val id: SessionId = SessionId.ZERO,
    val projectId: ProjectId,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Duration,
    val rowCount: RowCount,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
