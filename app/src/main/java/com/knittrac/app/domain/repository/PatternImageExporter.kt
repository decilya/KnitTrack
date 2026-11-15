package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern

/**
 * Контракт для генерации изображений (SVG) из схемы.
 * Отделяет Domain от конкретных форматов (SVG, PDF и т.д.).
 */
interface PatternImageExporter {
    /**
     * Генерирует SVG-строку для заданной схемы.
     *
     * @param pattern Схема для отрисовки.
     * @return [Result.Success] с SVG-строкой или [Result.Error], если схема пуста.
     */
    fun exportToSvg(pattern: Pattern): Result<String>
}
