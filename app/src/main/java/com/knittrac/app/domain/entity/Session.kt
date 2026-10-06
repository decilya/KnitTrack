package com.knittrac.app.domain.entity

import com.knittrac.app.core.common.SyncStatus

/**
 * Доменная модель сессии вязания.
 * 
 * ВАЖНО: Поля startTimestamp, endTimestamp и durationSeconds используются 
 * единообразно по всему проекту (и в Domain, и в Data слоях) для избежания 
 * путаницы при маппинге.
 * 
 * @property id Уникальный идентификатор сессии.
 * @property projectId Идентификатор проекта, к которому привязана сессия.
 * @property startTimestamp Время начала сессии (в миллисекундах).
 * @property endTimestamp Время окончания сессии (в миллисекундах).
 * @property durationSeconds Длительность сессии в секундах.
 * @property rowCount Количество связанных рядов, выполненных за сессию.
 * @property updatedAt Временная метка последнего обновления сессии.
 * @property syncStatus Статус синхронизации сессии (по умолчанию PENDING).
 */
data class Session(
    val id: Long = 0L,
    val projectId: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Long,
    val rowCount: Int,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.PENDING.name
)
