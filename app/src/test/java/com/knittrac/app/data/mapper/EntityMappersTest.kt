package com.knittrac.app.data.mapper

import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.local.entity.ProjectEntity
import com.knittrac.app.data.local.entity.SessionEntity
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.Session
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Юнит-тесты для функций маппинга в [EntityMappers].
 *
 * Критически важно для предотвращения регрессий при изменении полей
 * в Domain-моделях или Entity-классах. Покрывает:
 * - Прямой маппинг Entity в Domain с конверсией примитивов в Value Objects.
 * - Обратный маппинг Domain в Entity с разворотом VO в примитивы.
 * - Round-trip Entity-Domain-Entity — гарантия отсутствия потерь.
 * - Все константы [Category], включая [Category.OTHER].
 * - Fallback на [Category.OTHER] при неизвестной строке из БД.
 */
class EntityMappersTest {

    /**
     * Прямой маппинг [ProjectEntity] в [Project] со всеми полями.
     */
    @Test
    fun projectEntityToDomain_mapsAllFields() {
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
        assertEquals("Свитер", domain.name.value)
        assertEquals(Category.KNITTING, domain.category)
        assertEquals(1000L, domain.createdAt)
        assertEquals(2000L, domain.updatedAt)
        assertEquals(3600L, domain.totalTimeSeconds.seconds)
        assertEquals(SyncStatus.PENDING, domain.syncStatus)
    }

    /**
     * Обратный маппинг [Project] в [ProjectEntity], VO разворачиваются
     * в примитивы, enum — в строку.
     */
    @Test
    fun projectToEntity_mapsAllFieldsAndConvertsEnumToString() {
        val domain = Project(
            id = 2L,
            name = ProjectName.create("Шарф"),
            category = Category.CROCHET,
            createdAt = 3000L,
            updatedAt = 4000L,
            totalTimeSeconds = Duration.create(1800L),
            syncStatus = SyncStatus.SYNCED
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

    /**
     * Round-trip: [ProjectEntity] в [Project] в [ProjectEntity]
     * не теряет данные.
     */
    @Test
    fun projectRoundTrip_preservesAllFields() {
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

    /**
     * Все константы [Category] корректно проходят round-trip.
     */
    @Test
    fun allCategoryValues_roundTripCorrectly() {
        Category.values().forEach { category ->
            val entity = Project(
                id = 1L,
                name = ProjectName.create("Тест"),
                category = category,
                createdAt = 1L,
                updatedAt = 1L,
                totalTimeSeconds = Duration.create(1L),
                syncStatus = SyncStatus.PENDING
            ).toEntity()
            assertEquals(
                "Категория $category потерялась при round-trip",
                category,
                entity.toDomain().category
            )
        }
    }

    /**
     * Защита от corrupted DB: неизвестная категория в БД не должна
     * ронять приложение, а должна fallback-ить на Category.OTHER.
     */
    @Test
    fun projectEntityToDomain_fallsBackToOtherOnUnknownCategory() {
        val entity = ProjectEntity(
            id = 1L,
            name = "Свитер",
            category = "UNKNOWN_FUTURE_CATEGORY",
            createdAt = 1000L,
            updatedAt = 2000L,
            totalTimeSeconds = 3600L,
            syncStatus = "PENDING"
        )
        val domain = entity.toDomain()
        assertEquals(Category.OTHER, domain.category)
    }

    /**
     * Прямой маппинг [SessionEntity] в [Session] со всеми полями.
     */
    @Test
    fun sessionEntityToDomain_mapsAllFields() {
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
        assertEquals(10L, domain.durationSeconds.seconds)
        assertEquals(15, domain.rowCount)
        assertEquals(21000L, domain.updatedAt)
        assertEquals(SyncStatus.PENDING, domain.syncStatus)
    }

    /**
     * Обратный маппинг [Session] в [SessionEntity] со всеми полями.
     */
    @Test
    fun sessionToEntity_mapsAllFields() {
        val domain = Session(
            id = 20L,
            projectId = 6L,
            startTimestamp = 30000L,
            endTimestamp = 40000L,
            durationSeconds = Duration.create(10L),
            rowCount = 20,
            updatedAt = 41000L,
            syncStatus = SyncStatus.SYNCED
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

    /**
     * Round-trip: [SessionEntity] в [Session] в [SessionEntity]
     * не теряет данные.
     */
    @Test
    fun sessionRoundTrip_preservesAllFields() {
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
