package com.knittrac.app.core.localization

import android.content.Context
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Юнит-тесты для [SymbolLocalizer] (MVP: textGlyph).
 */
class SymbolLocalizerTest {

    private val context: Context = mockk(relaxed = true)
    private val localizer = SymbolLocalizer(context)

    @Test
    fun localize_returnsTextGlyph() {
        val symbol = Symbol(
            code = "K",
            displayName = "Лицевая",
            textGlyph = "V",
            category = SymbolCategory.STANDARD
        )
        assertEquals("V", localizer.localize(symbol))
    }
}
