package com.knittrac.app.data.mapper

import com.knittrac.app.domain.model.PatternSummary
import com.knittrac.app.data.local.entity.PatternEntity
import com.knittrac.app.data.local.entity.PatternRowEntity
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternDirection
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternName
import com.knittrac.app.domain.entity.PatternRepeat
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.Technique

/**
 * Маппер [PatternEntity] (Data слой) в [Pattern] (Domain слой).
 *
 * Легенда и раппорты передаются отдельно: их нет в самой [PatternEntity]
 * (отдельные таблицы `symbols` и `pattern_repeats`), и они загружаются
 * репозиторием параллельно с рядами.
 *
 * @param rows Ряды схемы, упорядоченные по rowNumber.
 * @param legend Символы легенды (системные + кастомные для этой схемы).
 * @param repeats Раппорты схемы.
 */
fun PatternEntity.toDomain(
    rows: List<PatternRowEntity>,
    legend: Set<Symbol> = emptySet(),
    repeats: List<PatternRepeat> = emptyList()
): Pattern = Pattern(
    id = PatternId.createOrDefault(id),
    name = PatternName.createOrDefault(name),
    technique = Technique.fromNameOrDefault(technique),
    direction = PatternDirection.fromNameOrDefault(direction),
    isReversible = isReversible,
    backgroundImageUri = backgroundImageUri,
    imageOffsetX = imageOffsetX,
    imageOffsetY = imageOffsetY,
    imageScale = imageScale,
    legend = legend,
    rows = rows.map { it.toDomain() },
    repeats = repeats
)

/**
 * Маппер [PatternRowEntity] (Data слой) в [PatternRow] (Domain слой).
 *
 * ВАЖНО: `operations` заведомо пуст — операции лежат в отдельной таблице
 * `pattern_operations` и загружаются через [PatternDao.getOperationsForRow]
 * (Шаг 7d, [AddPatternOperationService]). Для cross-aggregate проверки
 * «ряд принадлежит схеме» этого достаточно.
 */
fun PatternRowEntity.toDomain(): PatternRow = PatternRow(
    id = PatternRowId.createOrDefault(id),
    rowNumber = RowNumber.createOrDefault(rowNumber),
    operations = emptyList()
)

/**
 * Лёгкая проекция [PatternEntity] → [PatternSummary] для селектора схем (7f-B).
 *
 * ISP: только `id + name`. Не загружает `rows`, `legend`, `repeats`.
 */
fun PatternEntity.toSummary(): PatternSummary = PatternSummary(
    id = PatternId.createOrDefault(id),
    name = PatternName.createOrDefault(name)
)
