package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Символ схемы (системный или кастомный).
 *
 * ВАЖНО: в Domain это data class, а не агрегат. Репозиторий [SymbolRepository]
 * появится в Шаге 7d, когда AddPatternOperationService будет загружать
 * символы. Сейчас Symbol не имеет собственного репозитория (YAGNI).
 *
 * Системный символ: ownerPatternId = null (глобальный справочник).
 * Кастомный символ: ownerPatternId != null (принадлежит одной схеме).
 *
 * Компромисс DDD: кастомные Symbol (ownerPatternId != null) семантически
 * принадлежат Pattern, но в Domain не входят в его агрегат. На уровне БД —
 * FK CASCADE, на уровне Domain — weak reference (загрузка через
 * SymbolRepository в 7d). Это осознанное решение: Symbol может быть
 * системным (не принадлежит ни одной схеме), поэтому не является частью
 * агрегата Pattern.
 *
 * @property code Уникальный код символа (PK). Стабилен, не меняется при
 *   переименовании в UI.
 * @property ownerPatternId Идентификатор схемы-владельца для кастомных
 *   символов. null — системный символ.
 * @property displayName Внутреннее название для редактора схем. НЕ для UI:
 *   локализация в Presentation через SymbolLocalizer (аналогично Category).
 * @property textGlyph Текстовый глиф (например, "V" для лицевой).
 * @property category Категория (STANDARD или CUSTOM).
 * @property defaultColumnSpan Ширина символа по умолчанию в колонках.
 * @property defaultRowSpan Высота символа по умолчанию в рядах.
 */
@Keep
data class Symbol(
    val code: String,
    val ownerPatternId: Long? = null,
    val displayName: String,
    val textGlyph: String,
    val category: SymbolCategory,
    val defaultColumnSpan: Span = Span.ONE,
    val defaultRowSpan: Span = Span.ONE
) {
    init {
        require(code.isNotBlank()) { "Код символа не может быть пустым" }
        require(displayName.isNotBlank()) { "Название символа не может быть пустым" }
        require(textGlyph.isNotEmpty()) { "Глиф символа не может быть пустым" }
    }
}
