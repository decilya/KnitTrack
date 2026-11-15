package com.knittrac.app.data.renderer

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.ErrorCode
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.repository.PatternImageExporter
import javax.inject.Inject

/**
 * Реализация [PatternImageExporter], генерирующая SVG-строку.
 *
 * Использует StringBuilder для потоковой сборки.
 */
class SvgPatternRenderer @Inject constructor() : PatternImageExporter {

    override fun exportToSvg(pattern: Pattern): Result<String> {
        if (pattern.rows.isEmpty()) {
            return Result.Error(AppError.ValidationError(ErrorCode.SCHEMA_EMPTY, "Схема пуста"))
        }

        val sb = StringBuilder()
        val cellSize = 32
        val padding = 50
        
        appendHeader(sb, pattern, cellSize, padding)
        appendOperations(sb, pattern, cellSize, padding)
        
        sb.append("</svg>\n")
        return Result.Success(sb.toString())
    }

    private fun appendHeader(sb: StringBuilder, pattern: Pattern, cellSize: Int, padding: Int) {
        val maxColumns = pattern.rows.maxOfOrNull { row ->
            row.operations.maxOfOrNull { it.columnStart.value + it.effectiveColumnSpan.value } ?: 0
        } ?: 0
        val width = padding + maxColumns * cellSize
        val height = padding + pattern.rows.size * cellSize

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">\n")
        sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"#FFFFFF\" />\n")
    }

    private fun appendOperations(sb: StringBuilder, pattern: Pattern, cellSize: Int, padding: Int) {
        pattern.rows.forEachIndexed { rowIndex, row ->
            val y = padding + rowIndex * cellSize
            row.operations.forEach { op ->
                val x = padding + op.columnStart.value * cellSize
                val w = op.effectiveColumnSpan.value * cellSize
                val h = op.effectiveRowSpan.value * cellSize
                val color = op.colorHex ?: "#FFFFFF"
                
                sb.append("  <rect x=\"$x\" y=\"$y\" width=\"$w\" height=\"$h\" fill=\"$color\" stroke=\"#000000\" stroke-width=\"1\" />\n")
                val textX = x + w / 2
                val textY = y + h / 2
                sb.append("  <text x=\"$textX\" y=\"$textY\" font-family=\"Arial\" font-size=\"16\" fill=\"#000000\" text-anchor=\"middle\" dominant-baseline=\"central\">${op.symbol.textGlyph}</text>\n")
            }
        }
    }
}
