package com.knittrac.app.core.localization

import android.content.Context
import com.knittrac.app.R
import com.knittrac.app.domain.entity.Category
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты для [CategoryLocalizer].
 *
 * Проверяют маппинг каждой Category на соответствующий R.string.category_*,
 * а также устойчивость к копипасте и stateless-поведение.
 *
 * Context мокается через MockK — тест не требует Android runtime.
 * Значения строк подставляются через every { ... } returns "...",
 * поэтому тест устойчив к изменению текста в strings.xml.
 */
class CategoryLocalizerTest {

    private lateinit var context: Context
    private lateinit var localizer: CategoryLocalizer

    @Before
    fun setUp() {
        context = mockk()
        localizer = CategoryLocalizer(context)
    }

    /**
     * KNITTING должен резолвиться в R.string.category_knitting.
     * Проверяет и результат, и то, что Context.getString вызван ровно один раз
     * с правильным id — это защищает от случайной подмены ресурса в when.
     */
    @Test
    fun `localize KNITTING returns category_knitting string`() {
        every { context.getString(R.string.category_knitting) } returns "Вязание спицами"

        val result = localizer.localize(Category.KNITTING)

        assertEquals("Вязание спицами", result)
        verify(exactly = 1) { context.getString(R.string.category_knitting) }
    }

    /**
     * CROCHET должен резолвиться в R.string.category_crochet.
     */
    @Test
    fun `localize CROCHET returns category_crochet string`() {
        every { context.getString(R.string.category_crochet) } returns "Вязание крючком"

        val result = localizer.localize(Category.CROCHET)

        assertEquals("Вязание крючком", result)
        verify(exactly = 1) { context.getString(R.string.category_crochet) }
    }

    /**
     * BEADING должен резолвиться в R.string.category_beading.
     */
    @Test
    fun `localize BEADING returns category_beading string`() {
        every { context.getString(R.string.category_beading) } returns "Бисероплетение"

        val result = localizer.localize(Category.BEADING)

        assertEquals("Бисероплетение", result)
        verify(exactly = 1) { context.getString(R.string.category_beading) }
    }

    /**
     * MACRAME должен резолвиться в R.string.category_macrame.
     */
    @Test
    fun `localize MACRAME returns category_macrame string`() {
        every { context.getString(R.string.category_macrame) } returns "Макраме"

        val result = localizer.localize(Category.MACRAME)

        assertEquals("Макраме", result)
        verify(exactly = 1) { context.getString(R.string.category_macrame) }
    }

    /**
     * OTHER должен резолвиться в R.string.category_other.
     */
    @Test
    fun `localize OTHER returns category_other string`() {
        every { context.getString(R.string.category_other) } returns "Другое"

        val result = localizer.localize(Category.OTHER)

        assertEquals("Другое", result)
        verify(exactly = 1) { context.getString(R.string.category_other) }
    }

    /**
     * Все 5 категорий должны возвращать разные строки.
     *
     * Защищает от копипасты в when: если две ветки случайно возвращают
     * один и тот же ресурс, пользователь увидит одинаковые категории
     * в списке проектов, и это не будет заметно без теста.
     *
     * Проверяем через Set: размер множества должен совпасть с числом
     * категорий.
     */
    @Test
    fun `all categories return distinct strings`() {
        every { context.getString(R.string.category_knitting) } returns "Вязание спицами"
        every { context.getString(R.string.category_crochet) } returns "Вязание крючком"
        every { context.getString(R.string.category_beading) } returns "Бисероплетение"
        every { context.getString(R.string.category_macrame) } returns "Макраме"
        every { context.getString(R.string.category_other) } returns "Другое"

        val results = Category.entries.map { localizer.localize(it) }

        assertEquals(
            "Все категории должны возвращать разные строки",
            Category.entries.size,
            results.toSet().size
        )
    }

    /**
     * Повторный вызов с той же категорией возвращает тот же результат.
     *
     * Гарантия stateless: CategoryLocalizer не кэширует ничего между
     * вызовами и не зависит от порядка обращений. Важно для случая,
     * когда UI перерисовывается несколько раз при скролле списка.
     */
    @Test
    fun `localize is stateless — repeated calls return same value`() {
        every { context.getString(R.string.category_knitting) } returns "Вязание спицами"

        val first = localizer.localize(Category.KNITTING)
        val second = localizer.localize(Category.KNITTING)

        assertEquals(first, second)
    }
}
