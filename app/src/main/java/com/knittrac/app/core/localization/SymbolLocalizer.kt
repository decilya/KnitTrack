package com.knittrac.app.core.localization

import android.content.Context
import com.knittrac.app.domain.entity.Symbol
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Локализатор символов схем.
 *
 * MVP (7f-B): возвращает [Symbol.textGlyph] — текстовый глиф, уже
 * локализованный по смыслу (символ «V» = лицевая, не зависит от языка UI).
 *
 * P3: локализация `displayName` (внутреннего названия) — если понадобится
 * для палитры. Сейчас `displayName` — для редактора схем, не для UI.
 *
 * Эталон — [CategoryLocalizer] (`@Singleton`, `@ApplicationContext`).
 */
@Singleton
class SymbolLocalizer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Возвращает текстовый глиф символа.
     *
     * @param symbol Символ схемы.
     * @return [Symbol.textGlyph] (например, "V" для лицевой).
     */
    fun localize(symbol: Symbol): String = symbol.textGlyph
}
