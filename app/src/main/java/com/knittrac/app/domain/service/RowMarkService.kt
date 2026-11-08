package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.ProjectId
import com.knittrac.app.domain.entity.StitchCount
import com.knittrac.app.domain.model.MarkResult
import com.knittrac.app.domain.repository.PatternRepository
import com.knittrac.app.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * Domain Service: отметка ряда схемы как связанного (P1-B, 02.11.2026).
 *
 * Единственная публичная ответственность: [markRow].
 * Инварианты:
 * - `project.patternId != null`;
 * - `row.id ∈ pattern.rows` (cross-aggregate membership);
 * - `Project.markRow` проверяет денормализацию (B, 7g-C).
 *
 * Разбит из [ProgressService] (9 методов, 2 ответственности):
 * - [RowMarkService] — «отметить ряд» (этот файл).
 * - [NextRowService] — «продвинуть на следующий ряд».
 *
 * @property patternRepository Загрузка Pattern (membership row).
 * @property projectRepository Запись прогресса (updateProjectProgress).
 */
class RowMarkService @Inject constructor(
    private val patternRepository: PatternRepository,
    private val projectRepository: ProjectRepository
) {

    /**
     * Отмечает [row] как текущий ряд [project].
     *
     * @param project Проект.
     * @param row Ряд схемы, которую пользователь отмечает.
     * @return [Result.Success] с [MarkResult] (Added / AlreadyExists);
     *   [Result.Error] с [AppError.ValidationError] если у проекта нет схемы,
     *   ряд не принадлежит схеме, или [AppError.DatabaseError] при сбое БД.
     */
    suspend fun markRow(project: Project, row: PatternRow): Result<MarkResult> {
        return when (val markResult = project.markRow(row)) {
            is Result.Error -> markResult
            is Result.Success -> when (markResult.data) {
                is MarkResult.AlreadyExists -> markResult
                is MarkResult.Added -> addRow(project, row)
            }
        }
    }

    /**
     * Загружает Pattern по patternId проекта и проверяет membership [row].
     */
    private suspend fun addRow(project: Project, row: PatternRow): Result<MarkResult> {
        val patternId = project.patternId
            ?: return Result.Error(AppError.ValidationError("У проекта нет привязанной схемы"))

        return when (val patternResult = patternRepository.getPatternById(patternId)) {
            is Result.Error -> patternResult
            is Result.Success -> validateMembership(patternResult.data, row, project.id)
        }
    }

    /**
     * Проверяет, что [row] принадлежит [pattern]. Иначе — ValidationError.
     */
    private suspend fun validateMembership(
        pattern: Pattern,
        row: PatternRow,
        projectId: ProjectId
    ): Result<MarkResult> {
        return if (pattern.rows.none { it.id == row.id }) {
            Result.Error(AppError.ValidationError("Ряд не принадлежит схеме проекта"))
        } else {
            updateProgress(projectId, row)
        }
    }

    /**
     * Пишет прогресс в БД: currentPatternRowId = row.id,
     * currentRowNumber = row.rowNumber, currentStitch = 0.
     *
     * `currentStitch = 0` — потому что подсчёт петель per-stitch пока
     * не реализован (P3: per-stitch tracking в 7f-D+).
     */
    private suspend fun updateProgress(projectId: ProjectId, row: PatternRow): Result<MarkResult> {
        return when (val updateResult = projectRepository.updateProjectProgress(
            projectId = projectId,
            currentPatternRowId = row.id,
            currentStitch = StitchCount.ZERO
        )) {
            is Result.Error -> updateResult
            is Result.Success -> Result.Success(MarkResult.Added)
        }
    }
}
