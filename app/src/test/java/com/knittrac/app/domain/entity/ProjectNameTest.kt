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
    fun createReturnsProjectNameWithSameValueForValidInput() {
        val name = ProjectName.create("Свитер")

        assertEquals("Свитер", name.value)
    }

    /**
     * create применяет trim: ведущие и замыкающие пробелы удаляются.
     */
    @Test
    fun createTrimsLeadingAndTrailingWhitespace() {
        val name = ProjectName.create("   Шарф   ")

        assertEquals("Шарф", name.value)
    }

    /**
     * create на пустой строке бросает IllegalArgumentException.
     */
    @Test
    fun createThrowsOnEmptyString() {
        assertThrows(IllegalArgumentException::class.java) {
            ProjectName.create("")
        }
    }

    /**
     * create на строке из одних пробелов бросает IllegalArgumentException.
     */
    @Test
    fun createThrowsOnWhitespaceOnlyString() {
        assertThrows(IllegalArgumentException::class.java) {
            ProjectName.create("     ")
        }
    }

    /**
     * create на строке ровно MAX_LENGTH символов работает.
     */
    @Test
    fun createAcceptsStringOfExactlyMAXLENGTHCharacters() {
        val input = "а".repeat(ProjectName.MAX_LENGTH)

        val name = ProjectName.create(input)

        assertEquals(ProjectName.MAX_LENGTH, name.value.length)
    }

    /**
     * create на строке длиннее MAX_LENGTH бросает IllegalArgumentException.
     */
    @Test
    fun createThrowsOnStringLongerThanMAXLENGTH() {
        val input = "а".repeat(ProjectName.MAX_LENGTH + 1)

        assertThrows(IllegalArgumentException::class.java) {
            ProjectName.create(input)
        }
    }

    /**
     * createOrNull на невалидной строке возвращает null без исключения.
     */
    @Test
    fun createOrNullReturnsNullOnInvalidInput() {
        assertNull(ProjectName.createOrNull(""))
        assertNull(ProjectName.createOrNull("   "))
        assertNull(ProjectName.createOrNull("а".repeat(ProjectName.MAX_LENGTH + 1)))
    }

    /**
     * createOrNull на валидной строке возвращает ProjectName.
     */
    @Test
    fun createOrNullReturnsProjectNameOnValidInput() {
        val name = ProjectName.createOrNull("  Кофта  ")

        assertNotNull(name)
        assertEquals("Кофта", name?.value)
    }

    /**
     * createOrDefault на невалидной строке возвращает DEFAULT.
     */
    @Test
    fun createOrDefaultReturnsDEFAULTOnInvalidInput() {
        assertEquals(ProjectName.DEFAULT, ProjectName.createOrDefault(""))
        assertEquals(ProjectName.DEFAULT, ProjectName.createOrDefault("   "))
    }

    /**
     * createOrDefault на валидной строке возвращает ProjectName с этим значением.
     */
    @Test
    fun createOrDefaultReturnsProjectNameOnValidInput() {
        val name = ProjectName.createOrDefault("Плед")

        assertEquals("Плед", name.value)
    }

    /**
     * DEFAULT содержит нейтральный текст «Без названия».
     * Это значение используется при миграции пустых имён.
     */
    @Test
    fun dEFAULTHasFallbackValueBezNazvaniya() {
        assertEquals("Untitled", ProjectName.DEFAULT.value)
    }

    /**
     * MAX_LENGTH — публичный контракт. Фиксируем значение,
     * чтобы случайное изменение не сломало валидацию UI.
     */
    @Test
    fun mAXLENGTHIs100() {
        assertEquals(100, ProjectName.MAX_LENGTH)
    }

    /**
     * value class с одинаковым value равны по структурному равенству.
     */
    @Test
    fun projectNameWithSameValueIsEqual() {
        val a = ProjectName.create("Свитер")
        val b = ProjectName.create("Свитер")

        assertEquals(a, b)
    }
}
