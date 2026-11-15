package com.knittrack.app.data.renderer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.knittrack.app.core.common.AppError
import com.knittrack.app.core.common.ErrorCode
import com.knittrack.app.core.common.Result
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.repository.PdfPatternExporter
import java.io.OutputStream
import javax.inject.Inject

/**
 * Реализация [PdfPatternExporter] на базе Android [PdfDocument].
 */
class PdfPatternRenderer @Inject constructor() : PdfPatternExporter {

    override fun exportToPdf(pattern: Pattern, outputStream: OutputStream): Result<Unit> {
        if (pattern.rows.isEmpty()) {
            return Result.Error(AppError.ValidationError(ErrorCode.SCHEMA_EMPTY, "Схема пуста"))
        }

        return try {
            val pdfDocument = PdfDocument()
            val page = createPage(pdfDocument, pattern)
            drawContent(page.canvas, pattern)
            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.IoError(e))
        }
    }

    /**
     * Создаёт страницу PDF нужного размера (исходя из схемы).
     */
    private fun createPage(document: PdfDocument, pattern: Pattern): PdfDocument.Page {
        val maxColumns = pattern.rows.maxOfOrNull { row ->
            row.operations.maxOfOrNull { it.columnStart.value + it.effectiveColumnSpan.value } ?: 0
        } ?: 0
        val width = PADDING + maxColumns * CELL_SIZE
        val height = PADDING + pattern.rows.size * CELL_SIZE
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        return document.startPage(pageInfo)
    }

    /**
     * Рисует все операции схемы на Canvas.
     */
    private fun drawContent(canvas: Canvas, pattern: Pattern) {
        canvas.drawColor(Color.WHITE)
        val paint = Paint()

        pattern.rows.forEachIndexed { rowIndex, row ->
            val y = PADDING + rowIndex * CELL_SIZE
            row.operations.forEach { op ->
                drawOperation(canvas, paint, op, y)
            }
        }
    }

    /**
     * Рисует одну операцию: фон, рамку и символ.
     */
    private fun drawOperation(canvas: Canvas, paint: Paint, op: com.knittrack.app.domain.entity.PatternOperation, rowY: Int) {
        val x = PADDING + op.columnStart.value * CELL_SIZE
        val w = op.effectiveColumnSpan.value * CELL_SIZE
        val h = op.effectiveRowSpan.value * CELL_SIZE

        // Фон операции (или белый, если цвет не задан/битый)
        paint.color = try {
            if (op.colorHex != null) Color.parseColor(op.colorHex) else Color.WHITE
        } catch (e: IllegalArgumentException) {
            Color.WHITE
        }
        paint.style = Paint.Style.FILL
        canvas.drawRect(x.toFloat(), rowY.toFloat(), (x + w).toFloat(), (rowY + h).toFloat(), paint)

        // Рамка
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(x.toFloat(), rowY.toFloat(), (x + w).toFloat(), (rowY + h).toFloat(), paint)
        paint.style = Paint.Style.FILL

        // Символ по центру
        paint.color = Color.BLACK
        paint.textSize = 16f
        paint.textAlign = Paint.Align.CENTER
        val textY = rowY + (h / 2f) + 6f
        canvas.drawText(op.symbol.textGlyph, x + (w / 2f), textY, paint)
    }

    /**
     * Константы для рендеринга PDF.
     */
    companion object {
        /** Размер одной ячейки в пикселях. */
        private const val CELL_SIZE = 32

        /** Отступ для номеров рядов/колонок. */
        private const val PADDING = 50
    }
}
