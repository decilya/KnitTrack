package com.knittrac.app.data.mapper

import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.core.common.SyncStatus

object ProjectMapper {
    /**
     * Маппинг в доменную модель Project.
     * Адаптируйте параметры под ваш DTO, если этот маппер используется для сети.
     * Главное - соблюдать сигнатуру конструктора Project.
     */
    fun mapToDomain(
        id: Long = 0,
        name: String,
        category: Category,
        createdAt: Long,
        updatedAt: Long,
        syncStatus: String = SyncStatus.PENDING.name
    ): Project {
        return Project(
            id = id,
            name = name,
            category = category,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncStatus = syncStatus
        )
    }
}
