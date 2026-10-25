package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.RowCount
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.StitchCount
import com.knittrac.app.domain.model.MarkResult
import com.knittrac.app.domain.repository.PatternRepository
import com.knittrac.app.domain.repository.ProjectRepository
import javax.inject.Inject

class ProgressService @Inject constructor(
    private val patternRepository: PatternRepository,
    private val projectRepository: ProjectRepository
) {

    suspend fun markRow(project: Project, row: PatternRow): Result<MarkResult> {
        return when (val markResult = project.markRow(row)) {
            is Result.Error -> markResult
            is Result.Success -> when (markResult.data) {
                is MarkResult.AlreadyExists -> markResult
                is MarkResult.Added -> addRow(project, row)
            }
        }
    }

    private suspend fun addRow(project: Project, row: PatternRow): Result<MarkResult> {
        val patternId = project.patternId
            ?: return Result.Error(AppError.ValidationError("У проекта нет привязанной схемы"))

        return when (val patternResult = patternRepository.getPatternById(patternId)) {
            is Result.Error -> patternResult
            is Result.Success -> validateMembership(patternResult.data, row, project.id)
        }
    }

    private suspend fun validateMembership(
        pattern: Pattern,
        row: PatternRow,
        projectId: Long
    ): Result<MarkResult> {
        return if (pattern.rows.none { it.id == row.id }) {
            Result.Error(AppError.ValidationError("Ряд не принадлежит схеме проекта"))
        } else {
            updateProgress(projectId, row)
        }
    }

    private suspend fun updateProgress(projectId: Long, row: PatternRow): Result<MarkResult> {
        return when (val updateResult = projectRepository.updateProjectProgress(
            projectId = projectId,
            currentPatternRowId = row.id,
            currentRowNumber = RowCount.createOrDefault(row.rowNumber.value),
            currentStitch = StitchCount.ZERO
        )) {
            is Result.Error -> updateResult
            is Result.Success -> Result.Success(MarkResult.Added)
        }
    }

    /**
     * Отмечает следующий ряд схемы как связанный.
     *
     * «Следующий» = ряд с `rowNumber + 1` после текущего
     * ([Project.currentPatternRowId]); если текущего нет — первый ряд схемы.
     *
     * Крайние случаи возвращают [Result.Error] с [AppError.ValidationError]:
     * - у проекта нет `patternId` (нет привязанной схемы);
     * - у схемы нет рядов;
     * - `currentPatternRowId` указывает на последний ряд;
     * - `currentPatternRowId` не найден в `pattern.rows` (рассинхронизация).
     *
     * В отличие от [markRow] (возвращает [MarkResult]), здесь возвращается
     * [RowNumber] отмеченного ряда: `AlreadyExists` невозможен по определению
     * «следующего», а номер нужен UI сразу (без повторного запроса проекта).
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
            ?: return Result.Error(AppError.ValidationError("У проекта нет привязанной схемы"))

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
            return Result.Error(AppError.ValidationError("Схема не содержит рядов"))
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
                AppError.ValidationError("Текущий ряд id=${currentId.value} не найден в схеме")
            )
            currentIndex == pattern.rows.lastIndex -> Result.Error(
                AppError.ValidationError("Текущий ряд последний в схеме")
            )
            else -> Result.Success(pattern.rows[currentIndex + 1])
        }
    }

    /**
     * Делегирует в [markRow] и оборачивает результат в [RowNumber].
     */
    private suspend fun markSelectedRow(project: Project, row: PatternRow): Result<RowNumber> {
        return when (val markResult = markRow(project, row)) {
            is Result.Error -> markResult
            is Result.Success -> Result.Success(row.rowNumber)
        }
    }
}
