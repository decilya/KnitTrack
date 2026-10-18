package com.knittrac.app.data.mapper

import com.knittrac.app.core.common.SyncStatus
import com.knittrac.app.data.dto.ExportDataDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Duration
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectName
import com.knittrac.app.domain.entity.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для функций маппинга в [DtoMappers].
 *
 * Критически важно для формата бэкапа: любое изменение маппинга ломает
 * совместимость со старыми JSON-файлами. Покрывает:
 * - Прямой маппинг Domain в DTO с разворотом Value Objects в примитивы.
 * - Обратный маппинг DTO в Domain с валидацией через VO.
 * - Round-trip Domain-DTO-Domain — гарантия отсутствия потерь.
 * - Fallback на [Category.OTHER] при неизвестном значении category.
 */
class DtoMappersTest {

    // --- Тесты для Project ---

    /** Прямой маппинг [Project] в [ProjectDto] со всеми полями. */
    @Test
    fun projectToDto_mapsAllFieldsAndConvertsEnumToString() {
        val domain = Project(
            id = 1L,
            name = ProjectName.create("Свитер"),
            category = Category.KNITTING,
            createdAt = 1000L,
            updatedAt = 2000L,
            totalTimeSeconds = Duration.create(3600L),
            syncStatus = SyncStatus.PENDING
        )

        val dto = domain.toDto()

        assertEquals(1L, dto.id)
        assertEquals("Свитер", dto.name)
        assertEquals(Category.KNITTING.name, dto.category)
        assertEquals(1000L, dto.createdAt)
        assertEquals(2000L, dto.updatedAt)
        assertEquals(3600L, dto.totalTimeSeconds)
        assertEquals("PENDING", dto.syncStatus)
    }

    /** Обратный маппинг [ProjectDto] в [Project], включая String в Enum и VO. */
    @Test
    fun projectDtoToDomain_mapsAllFieldsAndConvertsStringToEnum() {
        val dto = ProjectDto(
            id = 2L,
            name = "Шарф",
            category = Category.CROCHET.name,
            createdAt = 3000L,
            updatedAt = 4000L,
            totalTimeSeconds = 1800L,
            syncStatus = "SYNCED"
        )

        val domain = dto.toDomain()

        assertEquals(2L, domain.id)
        assertEquals("Шарф", domain.name.value)
        assertEquals(Category.CROCHET, domain.category)
        assertEquals(3000L, domain.createdAt)
        assertEquals(4000L, domain.updatedAt)
        assertEquals(1800L, domain.totalTimeSeconds.seconds)
        assertEquals(SyncStatus.SYNCED, domain.syncStatus)
    }

    /**
     * Неизвестное значение category (опечатка в JSON, старый бэкап)
     * должно давать fallback на [Category.OTHER], а не крашить импорт.
     */
    @Test
    fun projectDtoToDomain_fallsBackToOtherOnUnknownCategory() {
        val dto = ProjectDto(
            id = 3L,
            name = "Повреждённый",
            category = "INVALID_VALUE",
            createdAt = 1L,
            updatedAt = 1L,
            totalTimeSeconds = 0L,
            syncStatus = "PENDING"
        )

        val domain = dto.toDomain()

        assertEquals(Category.OTHER, domain.category)
    }

    /** Round-trip: [Project] в [ProjectDto] в [Project] не теряет данные. */
    @Test
    fun projectRoundTrip_preservesAllFields() {
        val original = Project(
            id = 42L,
            name = ProjectName.create("Макраме-панно"),
            category = Category.MACRAME,
            createdAt = 111L,
            updatedAt = 222L,
            totalTimeSeconds = Duration.create(333L),
            syncStatus = SyncStatus.PENDING
        )

        assertEquals(original, original.toDto().toDomain())
    }

    /** Все константы [Category] корректно проходят round-trip через DTO. */
    @Test
    fun allCategoriesRoundTripThroughDto_arePreserved() {
        Category.entries.forEach { category ->
            val domain = Project(
                id = 1L,
                name = ProjectName.create("Тест"),
                category = category,
                createdAt = 1L,
                updatedAt = 1L,
                totalTimeSeconds = Duration.create(1L),
                syncStatus = SyncStatus.PENDING
            )

            assertEquals(
                "Категория $category потерялась при round-trip через DTO",
                category,
                domain.toDto().toDomain().category
            )
        }
    }

    // --- Тесты для Session ---

    /** Прямой маппинг [Session] в [SessionDto] со всеми полями. */
    @Test
    fun sessionToDto_mapsAllFields() {
        val domain = Session(
            id = 10L,
            projectId = 5L,
            startTimestamp = 10000L,
            endTimestamp = 20000L,
            durationSeconds = Duration.create(10L),
            rowCount = 15,
            updatedAt = 21000L,
            syncStatus = SyncStatus.PENDING
        )

        val dto = domain.toDto()

        assertEquals(10L, dto.id)
        assertEquals(5L, dto.projectId)
        assertEquals(10000L, dto.startTimestamp)
        assertEquals(20000L, dto.endTimestamp)
        assertEquals(10L, dto.durationSeconds)
        assertEquals(15, dto.rowCount)
        assertEquals(21000L, dto.updatedAt)
        assertEquals("PENDING", dto.syncStatus)
    }

    /** Обратный маппинг [SessionDto] в [Session] со всеми полями. */
    @Test
    fun sessionDtoToDomain_mapsAllFields() {
        val dto = SessionDto(
            id = 20L,
            projectId = 6L,
            startTimestamp = 30000L,
            endTimestamp = 40000L,
            durationSeconds = 10L,
            rowCount = 20,
            updatedAt = 41000L,
            syncStatus = "SYNCED"
        )

        val domain = dto.toDomain()

        assertEquals(20L, domain.id)
        assertEquals(6L, domain.projectId)
        assertEquals(30000L, domain.startTimestamp)
        assertEquals(40000L, domain.endTimestamp)
        assertEquals(10L, domain.durationSeconds.seconds)
        assertEquals(20, domain.rowCount)
        assertEquals(41000L, domain.updatedAt)
        assertEquals(SyncStatus.SYNCED, domain.syncStatus)
    }

    /** Round-trip: [Session] в [SessionDto] в [Session] не теряет данные. */
    @Test
    fun sessionRoundTrip_preservesAllFields() {
        val original = Session(
            id = 99L,
            projectId = 77L,
            startTimestamp = 1_000_000L,
            endTimestamp = 2_000_000L,
            durationSeconds = Duration.create(1000L),
            rowCount = 42,
            updatedAt = 2_000_001L,
            syncStatus = SyncStatus.PENDING
        )

        assertEquals(original, original.toDto().toDomain())
    }

    // --- Тесты для ExportDataDto ---

    /**
     * Фиксация текущей версии формата. Изменение этой строки — сигнал,
     * что старые бэкапы требуют миграции.
     */
    @Test
    fun currentVersion_isOneOne() {
        assertEquals("1.1", ExportDataDto.CURRENT_VERSION)
    }

    /**
     * projects и sessions должны иметь пустые списки по умолчанию,
     * чтобы старый бэкап без этих полей не падал с NPE.
     */
    @Test
    fun exportDataDto_hasEmptyListsByDefault() {
        val dto = ExportDataDto(
            version = ExportDataDto.CURRENT_VERSION,
            exportDate = 1234567890L
        )

        assertTrue(dto.projects.isEmpty())
        assertTrue(dto.sessions.isEmpty())
        assertTrue(dto.patterns.isEmpty())
        assertTrue(dto.patternRows.isEmpty())
    }
}
