package com.knittrac.app.data.mapper

import com.knittrac.app.data.dto.ExportDataDto
import com.knittrac.app.data.dto.ProjectDto
import com.knittrac.app.data.dto.SessionDto
import com.knittrac.app.domain.entity.Category
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для функций маппинга в [DtoMappers].
 *
 * Критически важно для формата бэкапа: любое изменение маппинга ломает
 * совместимость со старыми JSON-файлами. Покрывает:
 * - Прямой маппинг Domain → DTO.
 * - Обратный маппинг DTO → Domain, включая безопасный парсинг Category.
 * - Round-trip (Domain → DTO → Domain) — гарантия отсутствия потерь.
 * - Все константы [Category], включая fallback на [Category.OTHER]
 *   при неизвестном значении в JSON.
 */
class DtoMappersTest {

    // --- Тесты для Project ---

    /** Прямой маппинг [Project] → [ProjectDto] со всеми полями. */
    @Test
    fun `Project toDto маппит все поля и конвертирует Enum в String`() {
        val domain = Project(
            id = 1L,
            name = "Свитер",
            category = Category.KNITTING,
            createdAt = 1000L,
            updatedAt = 2000L,
            totalTimeSeconds = 3600L,
            syncStatus = "PENDING"
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

    /** Обратный маппинг [ProjectDto] → [Project], включая String → Enum. */
    @Test
    fun `ProjectDto toDomain маппит все поля и конвертирует String в Enum`() {
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
        assertEquals("Шарф", domain.name)
        assertEquals(Category.CROCHET, domain.category)
        assertEquals(3000L, domain.createdAt)
        assertEquals(4000L, domain.updatedAt)
        assertEquals(1800L, domain.totalTimeSeconds)
        assertEquals("SYNCED", domain.syncStatus)
    }

    /**
     * Неизвестное значение category (опечатка в JSON, старый бэкап)
     * должно давать fallback на [Category.OTHER], а не крашить импорт.
     */
    @Test
    fun `ProjectDto toDomain при неизвестной category возвращает OTHER`() {
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

    /** Round-trip: [Project] → [ProjectDto] → [Project] не теряет данные. */
    @Test
    fun `Project round-trip сохраняет все поля без потерь`() {
        val original = Project(
            id = 42L,
            name = "Макраме-панно",
            category = Category.MACRAME,
            createdAt = 111L,
            updatedAt = 222L,
            totalTimeSeconds = 333L,
            syncStatus = "PENDING"
        )

        assertEquals(original, original.toDto().toDomain())
    }

    /** Все константы [Category] корректно проходят маппинг Domain → DTO → Domain. */
    @Test
    fun `все категории Category проходят round-trip через DTO корректно`() {
        Category.entries.forEach { category ->
            val domain = Project(
                id = 1L,
                name = "Тест",
                category = category,
                createdAt = 1L,
                updatedAt = 1L,
                totalTimeSeconds = 1L,
                syncStatus = "PENDING"
            )

            assertEquals(
                "Категория $category потерялась при round-trip через DTO",
                category,
                domain.toDto().toDomain().category
            )
        }
    }

    // --- Тесты для Session ---

    /** Прямой маппинг [Session] → [SessionDto] со всеми полями. */
    @Test
    fun `Session toDto маппит все поля корректно`() {
        val domain = Session(
            id = 10L,
            projectId = 5L,
            startTimestamp = 10000L,
            endTimestamp = 20000L,
            durationSeconds = 10L,
            rowCount = 15,
            updatedAt = 21000L,
            syncStatus = "PENDING"
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

    /** Обратный маппинг [SessionDto] → [Session] со всеми полями. */
    @Test
    fun `SessionDto toDomain маппит все поля корректно`() {
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
        assertEquals(10L, domain.durationSeconds)
        assertEquals(20, domain.rowCount)
        assertEquals(41000L, domain.updatedAt)
        assertEquals("SYNCED", domain.syncStatus)
    }

    /** Round-trip: [Session] → [SessionDto] → [Session] не теряет данные. */
    @Test
    fun `Session round-trip сохраняет все поля без потерь`() {
        val original = Session(
            id = 99L,
            projectId = 77L,
            startTimestamp = 1_000_000L,
            endTimestamp = 2_000_000L,
            durationSeconds = 1000L,
            rowCount = 42,
            updatedAt = 2_000_001L,
            syncStatus = "PENDING"
        )

        assertEquals(original, original.toDto().toDomain())
    }

    // --- Тесты для ExportDataDto ---

    /**
     * Фиксация текущей версии формата. Изменение этой строки — сигнал,
     * что старые бэкапы требуют миграции.
     */
    @Test
    fun `CURRENT_VERSION равен 1_0`() {
        assertEquals("1.0", ExportDataDto.CURRENT_VERSION)
    }

    /**
     * projects и sessions должны иметь пустые списки по умолчанию,
     * чтобы старый бэкап без этих полей не падал с NPE.
     */
    @Test
    fun `ExportDataDto по умолчанию содержит пустые списки`() {
        val dto = ExportDataDto(
            version = ExportDataDto.CURRENT_VERSION,
            exportDate = 1234567890L
        )

        assertTrue(dto.projects.isEmpty())
        assertTrue(dto.sessions.isEmpty())
    }
}
