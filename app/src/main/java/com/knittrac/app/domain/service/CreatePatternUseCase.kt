package com.knittrac.app.domain.service

import com.knittrac.app.core.common.ErrorCode

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.repository.PatternRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Создаёт схему без рядов в Domain. Ряды пишет репозиторий: у них ещё нет id.
 */
class CreatePatternUseCase @Inject constructor(
    private val patternRepository: PatternRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<CreatePatternParams, PatternId>(dispatcher) {

    override suspend fun execute(params: CreatePatternParams): Result<PatternId> {
        if (params.rowCount.value < 1) {
            return Result.Error(AppError.ValidationError(ErrorCode.INVALID_ROW_COUNT))
        }
        val pattern = Pattern(
            id = PatternId.ZERO,
            name = params.name,
            technique = params.technique,
            direction = params.direction,
            isReversible = false
        )
        return patternRepository.addPattern(pattern, params.rowCount)
    }
}
