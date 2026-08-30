package com.knittrac.app.domain.entity

import com.knittrac.app.core.common.SyncStatus

/**
 * Доменная сущность сессии работы над проектом.
 *
 * Сессия представляет собой один непрерывный период работы над проектом
 * (например, "вязала шарф с 14:00 до 15:30"). Каждая сессия содержит:
 * - Временные метки начала и окончания
 * - Продолжительность в секундах
 * - Количество выполненных рядов (или другую метрику прогресса)
 *
 * Важные особенности:
 * - [durationSeconds] вычисляется как разница между [endTimestamp] и [startTimestamp]
 * - [syncStatus] устанавливается в PENDING при создании (Правило 3)
 * - [updatedAt] автоматически обновляется при сохранении
 *
 * @param id Уникальный идентификатор сессии (0 для новых сессий).
 * @param projectId Идентификатор проекта, к которому относится сессия.
 *        Внешний ключ для связи с таблицей проектов.
 * @param startTimestamp Временная метка начала сессии (в миллисекундах).
 * @param endTimestamp Временная метка окончания сессии (в миллисекундах).
 * @param durationSeconds Продолжительность сессии в секундах.
 *        Вычисляется как (endTimestamp - startTimestamp) / 1000.
 * @param rowCount Количество выполненных рядов (или другая метрика прогресса).
 *        Может быть 0, если пользователь не указал количество.
 * @param updatedAt Временная метка последнего обновления (в миллисекундах).
 *        Автоматически устанавливается при сохранении.
 * @param syncStatus Статус синхронизации с облачным сервером.
 *        Устанавливается в [SyncStatus.PENDING] при создании.
 *
 * @see Project Сущность проекта, к которому относится сессия.
 * @see SyncStatus Статусы синхронизации.
 */
data class Session(
    val id: Long = 0L,
    val projectId: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Long,
    val rowCount: Int,
    val updatedAt: Long,
    val syncStatus: SyncStatus
)