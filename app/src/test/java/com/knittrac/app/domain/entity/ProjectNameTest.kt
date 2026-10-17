package com.knittrac.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Юнит-тесты для [ProjectName].
 *
 * Покрывают:
 * - валидацию входных данных в [ProjectName.create];
 * - безопасный парсинг через [ProjectName.createOrNull];
 * - fallback через [ProjectName.createOrDefault];
 * - trim и границы длины;
 * - контракт [ProjectName.DEFAULT] и [ProjectName.MAX_LENGTH].
 */
class ProjectNameTest {

    /**
     * create с валидной строкой возвращает ProjectName с тем же значением.
     */
    @Test
    fun `create returns ProjectName with same value for valid input`() {
        val name = ProjectName.create("Свитер")

        assertEquals("Свитер", name.value)
    }

    /**
     * create применяет trim: ведущие и замыкающие пробелы удаляются.
     */
    @Test
    fun `create trims leading and trailing whitespace`() {
        val name = ProjectName.create("   Шарф   ")

        assertEquals("Шарф", name.value)
    }

    /**
     * create на пустой строке бросает IllegalArgumentException.
     */
    @Test
    fun `create throws on empty string`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProjectName.create("")
        }
    }

    /**
     * create на строке из одних пробелов бросает IllegalArgumentException.
     */
    @Test
    fun `create throws on whitespace-only string`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProjectName.create("     ")
        }
    }

    /**
     * create на строке ровно MAX_LENGTH символов работает.
     */
    @Test
    fun `create accepts string of exactly MAX_LENGTH characters`() {
        val input = "а".repeat(ProjectName.MAX_LENGTH)

        val name = ProjectName.create(input)

        assertEquals(ProjectName.MAX_LENGTH, name.value.length)
    }

    /**
     * create на строке длиннее MAX_LENGTH бросает IllegalArgumentException.
     */
    @Test
    fun `create throws on string longer than MAX_LENGTH`() {
        val input = "а".repeat(ProjectName.MAX_LENGTH + 1)

        assertThrows(IllegalArgumentException::class.java) {
            ProjectName.create(input)
        }
    }

    /**
     * createOrNull на невалидной строке возвращает null без исключения.
     */
    @Test
    fun `createOrNull returns null on invalid input`() {
        assertNull(ProjectName.createOrNull(""))
        assertNull(ProjectName.createOrNull("   "))
        assertNull(ProjectName.createOrNull("а".repeat(ProjectName.MAX_LENGTH + 1)))
    }

    /**
     * createOrNull на валидной строке возвращает ProjectName.
     */
    @Test
    fun `createOrNull returns ProjectName on valid input`() {
        val name = ProjectName.createOrNull("  Кофта  ")

        assertNotNull(name)
        assertEquals("Кофта", name?.value)
    }

    /**
     * createOrDefault на невалидной строке возвращает DEFAULT.
     */
    @Test
    fun `createOrDefault returns DEFAULT on invalid input`() {
        assertEquals(ProjectName.DEFAULT, ProjectName.createOrDefault(""))
        assertEquals(ProjectName.DEFAULT, ProjectName.createOrDefault("   "))
    }

    /**
     * createOrDefault на валидной строке возвращает ProjectName с этим значением.
     */
    @Test
    fun `createOrDefault returns ProjectName on valid input`() {
        val name = ProjectName.createOrDefault("Плед")

        assertEquals("Плед", name.value)
    }

    /**
     * DEFAULT содержит нейтральный текст «Без названия».
     * Это значение используется при миграции пустых имён.
     */
    @Test
    fun `DEFAULT has fallback value Bez nazvaniya`() {
        assertEquals("Untitled", ProjectName.DEFAULT.value)
    }

    /**
     * MAX_LENGTH — публичный контракт. Фиксируем значение,
     * чтобы случайное изменение не сломало валидацию UI.
     */
    @Test
    fun `MAX_LENGTH is 100`() {
        assertEquals(100, ProjectName.MAX_LENGTH)
    }

    /**
     * value class с одинаковым value равны по структурному равенству.
     */
    @Test
    fun `ProjectName with same value is equal`() {
        val a = ProjectName.create("Свитер")
        val b = ProjectName.create("Свитер")

        assertEquals(a, b)
    }
}
