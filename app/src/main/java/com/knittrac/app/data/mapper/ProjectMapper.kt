package com.knittrac.app.data.mapper
import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
object ProjectMapper {
    fun ProjectEntity.toDomain(): Project {
        val cat = runCatching { Category.valueOf(this.category) }.getOrDefault(Category.OTHER)
        val sync = runCatching { SyncStatus.valueOf(this.syncStatus) }.getOrDefault(SyncStatus.PENDING)
        return Project(id, name, cat, createdAt, totalTimeSeconds, updatedAt, sync)
    }
    fun Project.toEntity(): ProjectEntity = ProjectEntity(id, name, category.name, createdAt, totalTimeSeconds, updatedAt, syncStatus.name)
}
