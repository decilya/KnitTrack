package com.knittrac.app.domain.entity

import androidx.annotation.Keep

/**
 * Схема вязания.
 *
 * Immutable агрегат. Метод getDirectionForRow появится в Шаге 7b
 * (кэш Map<PatternRowId, Int> для ALTERNATING).
 *
 * ВАЖНО: fork — НЕ метод агрегата. Реализуется в ForkPatternService
 * (Application), см. ТЗ v8.0 §3.2.
 *
 * Инварианты:
 * - name валиден (PatternName).
 * - rows упорядочены по rowNumber по возрастанию (для детерминизма
 *   ALTERNATING). Дубликаты rowNumber невозможны: UNIQUE(pattern_id,
 *   row_number) в БД + строгая проверка здесь.
 * - repeats НЕ упорядочены: это справочник раппортов, порядок не семантичен.
 *   Уникальность по code проверяется отдельно.
 * - legend содержит уникальные Symbol по code. ВАЖНО: Set<Symbol> сам по
 *   себе НЕ гарантирует уникальность code (data class равен по всем полям),
 *   поэтому проверка уникальности code выполняется явно.
 *
 * @property id Идентификатор схемы.
 * @property name Название схемы.
 * @property technique Техника выполнения.
 * @property direction Направление чтения.
 * @property isReversible Двусторонняя ли схема (ЛС/ИС).
 * @property backgroundImageUri URI фонового изображения. String (не
 *   android.net.Uri — Persistence Ignorance), совместим с Coil
 *   (AsyncImage(model = String?)). null — без фона.
 * @property imageOffsetX Смещение фона по X (для Canvas-калибровки).
 *   Должно быть конечным числом (не NaN, не Infinity).
 * @property imageOffsetY Смещение фона по Y. Должно быть конечным числом.
 * @property imageScale Масштаб фона. Должен быть конечным числом > 0.
 * @property legend Множество символов, использованных в схеме (ссылки,
 *   не владение).
 * @property rows Ряды схемы, упорядоченные по rowNumber.
 * @property repeats Раппорты схемы.
 */
@Keep
data class Pattern(
    val id: PatternId,
    val name: PatternName,
    val technique: Technique,
    val direction: PatternDirection,
    val isReversible: Boolean,
    val backgroundImageUri: String? = null,
    val imageOffsetX: Float = 0f,
    val imageOffsetY: Float = 0f,
    val imageScale: Float = 1f,
    val legend: Set<Symbol> = emptySet(),
    val rows: List<PatternRow> = emptyList(),
    val repeats: List<PatternRepeat> = emptyList()
) {
    init {
        require(imageScale.isFinite() && imageScale > 0f) {
            "Масштаб фона должен быть конечным числом > 0"
        }
        require(imageOffsetX.isFinite()) { "Смещение фона по X должно быть конечным числом" }
        require(imageOffsetY.isFinite()) { "Смещение фона по Y должно быть конечным числом" }
        require(rows.zipWithNext().all { (a, b) -> a.rowNumber.value < b.rowNumber.value }) {
            "Pattern.rows должен быть упорядочен по rowNumber по возрастанию"
        }
        val legendCodes = legend.map { it.code }.toSet()
        require(legendCodes.size == legend.size) {
            "В legend не может быть двух Symbol с одинаковым code"
        }
        val repeatCodes = repeats.map { it.code }.toSet()
        require(repeatCodes.size == repeats.size) {
            "В repeats не может быть двух PatternRepeat с одинаковым code"
        }
    }
}
