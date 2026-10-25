package com.knittrac.app.domain.model

import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol

/**
 * Параметры добавления операции в ряд схемы.
 *
 * Объект-параметры вместо длинного списка аргументов: 8 полей,
 * читаемость вызова и стабильность сигнатуры при добавлении полей.
 *
 * @property patternId Схема-владелец (для загрузки агрегата [Pattern]).
 * @property row Ряд-владелец (id и rowNumber, откуда строится [PatternOperation]).
 * @property columnStart Начальная колонка (0-based).
 * @property symbol Символ операции (обязан быть в легенде схемы).
 * @property backSymbol Символ для изнаночной стороны (опционально).
 * @property colorHex Цвет в HEX (опционально).
 * @property overrideSpan Переопределение ширины (опционально).
 * @property repeatCode Код раппорта (опционально; если задан — должен
 *   присутствовать в [Pattern.repeats]).
 */
data class AddPatternOperationParams(
    val patternId: PatternId,
    val row: PatternRow,
    val columnStart: ColumnStart,
    val symbol: Symbol,
    val backSymbol: Symbol? = null,
    val colorHex: String? = null,
    val overrideSpan: Span? = null,
    val repeatCode: String? = null
)
