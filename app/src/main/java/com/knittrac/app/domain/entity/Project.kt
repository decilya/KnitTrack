package com.knittrac.app.domain.entity

/**
 * Доменная модель проекта вязания.
 * 
 * Представляет бизнес-сущность проекта, независимую от деталей хранения в БД.
 * 
 * @property id Уникальный идентификатор проекта.
 * @property name Название проекта.
 * @property category Категория проекта (например, "Вязание спицами").
 * @property createdAt Временная метка создания проекта (в миллисекундах).
 * @property totalTimeSeconds Общее затраченное на проект время в секундах.
 * @property updatedAt Временная метка последнего обновления проекта.
 * @property syncStatus Статус синхронизации данных с облаком.
 */
data class Project(
    val id: Long = 0L,
    val name: String,
    val category: Category,
    val createdAt: Long,
    val totalTimeSeconds: Long = 0L,
    val updatedAt: Long,
    val syncStatus: String
)
