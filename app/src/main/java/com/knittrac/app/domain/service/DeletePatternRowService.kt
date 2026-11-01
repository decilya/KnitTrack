package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.repository.PatternRepository
import javax.inject.Inject

/**
 * Сервис удаления ряда из схемы (7f-D-2).
 *
 * Проверяет, что ряд принадлежит схеме, затем делегирует удаление.
 * Сброс прогресса проектов, стоящих на этом ряду, выполняется атомарно
 * в [PatternRepository.deleteRow].
 */
class DeletePatternRowService @Inject constructor(
    private val patternRepository: PatternRepository
) {
    suspend fun delete(params: DeletePatternRowParams): Result<Unit> =
        when (val patternResult = patternRepository.getPatternById(params.patternId)) {
            is Result.Error -> patternResult
            is Result.Success -> deleteRowFromPattern(patternResult.data, params)
        }

    private suspend fun deleteRowFromPattern(
        pattern: Pattern,
        params: DeletePatternRowParams
    ): Result<Unit> {
        val rowExists = pattern.rows.any { it.id == params.rowId }
        return if (!rowExists) {
            Result.Error(
                AppError.NotFoundError(
                    "Ряд ${params.rowId.value} не найден в схеме ${params.patternId.value}"
                )
            )
        } else {
            patternRepository.deleteRow(params.rowId)
        }
    }
}
