package com.knittrac.app.domain.service

import com.knittrac.app.core.common.ErrorCode

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.repository.PatternRepository
import javax.inject.Inject

/**
 * Domain Service: продвижение проекта на следующий ряд схемы (P1-B, 02.11.2026).
 *
 * Единственная публичная ответственность: [markNextRow].
 *
 * «Следующий» = ряд с `rowNumber + 1` после текущего ([Project.currentPatternRowId]);
 * если текущего нет — первый ряд схемы.
 *
 * Крайние случаи → [AppError.ValidationError]:
 * - у проекта нет `patternId`;
 * - у схемы нет рядов;
 * - `currentPatternRowId` указывает на последний ряд;
 * - `currentPatternRowId` не найден в `pattern.rows` (рассинхронизация).
 *
 * Композиция: [markSelectedRow] делегирует в [RowMarkService.markRow]
 * (DRY: addRow + validateMembership + updateProgress — уже там).
 *
 * @property patternRepository Загрузка Pattern (выбор следующего ряда).
 * @property projectRepository Не используется напрямую (через [RowMarkService]).
 * @property rowMarkService Отметка выбранного ряда (композиция).
 */
class NextRowService @Inject constructor(
    private val patternRepository: PatternRepository,
    private val rowMarkService: RowMarkService
) {

    /**
     * Отмечает следующий ряд схемы как связанный.
     *
     * @param project Проект, чей прогресс нужно продвинуть.
     * @return [Result.Success] с номером отмеченного ряда;
     *   [Result.Error] с [AppError.ValidationError] при нарушении инварианта
     *   или [AppError.DatabaseError] / [AppError.NotFoundError] при сбое БД.
     */
    suspend fun markNextRow(project: Project): Result<RowNumber> {
        return when (val nextRowResult = selectNextRow(project)) {
            is Result.Error -> nextRowResult
            is Result.Success -> markSelectedRow(project, nextRowResult.data)
        }
    }

    /**
     * Загружает [Pattern] и выбирает следующий ряд для [project].
     */
    private suspend fun selectNextRow(project: Project): Result<PatternRow> {
        val patternId = project.patternId
            ?: return Result.Error(AppError.ValidationError(ErrorCode.PATTERN_NOT_ATTACHED))

        return when (val patternResult = patternRepository.getPatternById(patternId)) {
            is Result.Error -> patternResult
            is Result.Success -> pickNextRow(project, patternResult.data)
        }
    }

    /**
     * Выбирает «следующий» ряд из непустого [pattern].
     */
    private fun pickNextRow(project: Project, pattern: Pattern): Result<PatternRow> {
        if (pattern.rows.isEmpty()) {
            return Result.Error(AppError.ValidationError(ErrorCode.SCHEMA_EMPTY))
        }
        return pickFromNonEmpty(project, pattern)
    }

    /**
     * Если текущего ряда нет — первый ряд; иначе — следующий после текущего.
     */
    private fun pickFromNonEmpty(project: Project, pattern: Pattern): Result<PatternRow> {
        val currentId = project.currentPatternRowId
            ?: return Result.Success(pattern.rows.first())
        return findAndAdvance(pattern, currentId)
    }

    /**
     * Находит текущий ряд по id и возвращает следующий.
     */
    private fun findAndAdvance(
        pattern: Pattern,
        currentId: PatternRowId
    ): Result<PatternRow> {
        val currentIndex = pattern.rows.indexOfFirst { it.id == currentId }
        return when {
            currentIndex < 0 -> Result.Error(
                AppError.ValidationError(ErrorCode.PATTERN_ROW_NOT_FOUND_IN_SCHEMA, "id=${currentId.value}")
            )
            currentIndex == pattern.rows.lastIndex -> Result.Error(
                AppError.ValidationError(ErrorCode.CURRENT_ROW_LAST_IN_SCHEMA)
            )
            else -> Result.Success(pattern.rows[currentIndex + 1])
        }
    }

    /**
     * Отмечает выбранный ряд через [RowMarkService] и оборачивает в [RowNumber].
     *
     * `AlreadyExists` не провepяется: следующий ряд по определению не текущий.
     */
    private suspend fun markSelectedRow(project: Project, row: PatternRow): Result<RowNumber> {
        return when (val markResult = rowMarkService.markRow(project, row)) {
            is Result.Error -> markResult
            is Result.Success -> Result.Success(row.rowNumber)
        }
    }
}
