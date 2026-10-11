package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Доменная модель проекта вязания.
 *
 * Представляет бизнес-сущность проекта, независимую от деталей хранения в БД.
 * Название и общее время — Value Objects [ProjectName] и [Duration]:
 * защищают инварианты (непустое имя, неотрицательная длительность)
 * на уровне типа, а не в рантайме.
 *
 * @property id Уникальный идентификатор проекта.
 * @property name Название проекта.
 * @property category Категория проекта (например, "Вязание спицами").
 * @property createdAt Временная метка создания проекта (в миллисекундах).
 * @property totalTimeSeconds Общее затраченное на проект время.
 * @property updatedAt Временная метка последнего обновления проекта.
 * @property syncStatus Статус синхронизации данных с облаком.
 */
@Keep
data class Project(
    val id: Long = 0L,
    val name: ProjectName,
    val category: Category,
    val createdAt: Long,
    val totalTimeSeconds: Duration = Duration.ZERO,
    val updatedAt: Long,
    val syncStatus: String
)
