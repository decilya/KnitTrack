package com.knittrack.app.data.mapper

import com.knittrack.app.domain.model.PatternSummary
import com.knittrack.app.data.local.entity.PatternEntity
import com.knittrack.app.data.local.entity.PatternRowEntity
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.entity.PatternDirection
import com.knittrack.app.domain.entity.PatternId
import com.knittrack.app.domain.entity.PatternName
import com.knittrack.app.domain.entity.PatternRepeat
import com.knittrack.app.domain.entity.PatternRow
import com.knittrack.app.domain.entity.PatternRowId
import com.knittrack.app.domain.entity.RowNumber
import com.knittrack.app.domain.entity.Symbol
import com.knittrack.app.domain.entity.Technique

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
 * Метаданные схемы для INSERT в `patterns`. Ряды, легенда и раппорты
 * пишутся отдельными таблицами.
 */
fun Pattern.toEntity(): PatternEntity = PatternEntity(
    id = id.value,
    name = name.value,
    technique = technique.name,
    direction = direction.name,
    isReversible = isReversible,
    backgroundImageUri = backgroundImageUri,
    imageOffsetX = imageOffsetX,
    imageOffsetY = imageOffsetY,
    imageScale = imageScale
)

/** Ряд для INSERT. `patternId` в Domain не хранится — его передаёт владелец. */
fun PatternRow.toEntity(patternId: Long): PatternRowEntity = PatternRowEntity(
    id = id.value,
    patternId = patternId,
    rowNumber = rowNumber.value
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
