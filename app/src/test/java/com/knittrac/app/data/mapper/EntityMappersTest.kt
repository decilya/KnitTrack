package com.knittrac.app.data.mapper

import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Юнит-тесты для функций маппинга в [EntityMappers].
 *
 * Критически важно для предотвращения регрессий при изменении полей
 * в Domain-моделях или Entity-классах. Покрывает:
 * - Прямой маппинг Entity → Domain.
 * - Обратный маппинг Domain → Entity.
 * - Round-trip (Entity → Domain → Entity) — гарантия отсутствия потерь.
 * - Все константы [Category], включая [Category.OTHER].
 */
class EntityMappersTest {

    // --- Тесты для Project ---

    /** Прямой маппинг [ProjectEntity] → [Project] со всеми полями. */
    @Test
    fun `ProjectEntity toDomain маппит все поля корректно`() {
        val entity = ProjectEntity(
            id = 1L,
            name = "Свитер",
            category = Category.KNITTING.name,
            createdAt = 1000L,
            updatedAt = 2000L,
            totalTimeSeconds = 3600L,
            syncStatus = "PENDING"
        )
        val domain = entity.toDomain()
        assertEquals(1L, domain.id)
        assertEquals("Свитер", domain.name)
        assertEquals(Category.KNITTING, domain.category)
        assertEquals(1000L, domain.createdAt)
        assertEquals(2000L, domain.updatedAt)
        assertEquals(3600L, domain.totalTimeSeconds)
        assertEquals("PENDING", domain.syncStatus)
    }

    /** Обратный маппинг [Project] → [ProjectEntity], включая Enum → String. */
    @Test
    fun `Project toEntity маппит все поля и конвертирует Enum в String`() {
        val domain = Project(
            id = 2L,
            name = "Шарф",
            category = Category.CROCHET,
            createdAt = 3000L,
            updatedAt = 4000L,
            totalTimeSeconds = 1800L,
            syncStatus = "SYNCED"
        )
        val entity = domain.toEntity()
        assertEquals(2L, entity.id)
        assertEquals("Шарф", entity.name)
        assertEquals(Category.CROCHET.name, entity.category)
        assertEquals(3000L, entity.createdAt)
        assertEquals(4000L, entity.updatedAt)
        assertEquals(1800L, entity.totalTimeSeconds)
        assertEquals("SYNCED", entity.syncStatus)
    }

    /** Round-trip: [ProjectEntity] → [Project] → [ProjectEntity] не теряет данные. */
    @Test
    fun `Project round-trip сохраняет все поля без потерь`() {
        val original = ProjectEntity(
            id = 42L,
            name = "Макраме-панно",
            category = Category.MACRAME.name,
            createdAt = 111L,
            updatedAt = 222L,
            totalTimeSeconds = 333L,
            syncStatus = "PENDING"
        )
        assertEquals(original, original.toDomain().toEntity())
    }

    /** Все константы [Category] корректно проходят маппинг Domain → Entity → Domain. */
    @Test
    fun `все категории Category проходят round-trip корректно`() {
        Category.values().forEach { category ->
            val entity = Project(
                id = 1L,
                name = "Тест",
                category = category,
                createdAt = 1L,
                updatedAt = 1L,
                totalTimeSeconds = 1L,
                syncStatus = "PENDING"
            ).toEntity()
            assertEquals(
                "Категория $category потерялась при round-trip",
                category,
                entity.toDomain().category
            )
        }
    }

    // --- Тесты для Session ---

    /** Прямой маппинг [SessionEntity] → [Session] со всеми полями. */
    @Test
    fun `SessionEntity toDomain маппит все поля корректно`() {
        val entity = SessionEntity(
            id = 10L,
            projectId = 5L,
            startTimestamp = 10000L,
            endTimestamp = 20000L,
            durationSeconds = 10L,
            rowCount = 15,
            updatedAt = 21000L,
            syncStatus = "PENDING"
        )
        val domain = entity.toDomain()
        assertEquals(10L, domain.id)
        assertEquals(5L, domain.projectId)
        assertEquals(10000L, domain.startTimestamp)
        assertEquals(20000L, domain.endTimestamp)
        assertEquals(10L, domain.durationSeconds)
        assertEquals(15, domain.rowCount)
        assertEquals(21000L, domain.updatedAt)
        assertEquals("PENDING", domain.syncStatus)
    }

    /** Обратный маппинг [Session] → [SessionEntity] со всеми полями. */
    @Test
    fun `Session toEntity маппит все поля корректно`() {
        val domain = Session(
            id = 20L,
            projectId = 6L,
            startTimestamp = 30000L,
            endTimestamp = 40000L,
            durationSeconds = 10L,
            rowCount = 20,
            updatedAt = 41000L,
            syncStatus = "SYNCED"
        )
        val entity = domain.toEntity()
        assertEquals(20L, entity.id)
        assertEquals(6L, entity.projectId)
        assertEquals(30000L, entity.startTimestamp)
        assertEquals(40000L, entity.endTimestamp)
        assertEquals(10L, entity.durationSeconds)
        assertEquals(20, entity.rowCount)
        assertEquals(41000L, entity.updatedAt)
        assertEquals("SYNCED", entity.syncStatus)
    }

    /** Round-trip: [SessionEntity] → [Session] → [SessionEntity] не теряет данные. */
    @Test
    fun `Session round-trip сохраняет все поля без потерь`() {
        val original = SessionEntity(
            id = 99L,
            projectId = 77L,
            startTimestamp = 1_000_000L,
            endTimestamp = 2_000_000L,
            durationSeconds = 1000L,
            rowCount = 42,
            updatedAt = 2_000_001L,
            syncStatus = "PENDING"
        )
        assertEquals(original, original.toDomain().toEntity())
    }
}
