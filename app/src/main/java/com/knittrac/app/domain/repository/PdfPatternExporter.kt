package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import java.io.OutputStream

/**
 * Контракт для генерации PDF из схемы.
 */
interface PdfPatternExporter {
    /**
     * Генерирует PDF и записывает в [outputStream].
     *
     * @param pattern Схема для отрисовки.
     * @param outputStream Поток для записи PDF. Не закрывается здесь.
     * @return [Result.Success] или [Result.Error].
     */
    fun exportToPdf(pattern: Pattern, outputStream: OutputStream): Result<Unit>
}
