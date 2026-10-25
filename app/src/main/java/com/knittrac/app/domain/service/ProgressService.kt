package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.Project
import com.knittrac.app.domain.entity.RowCount
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
}
