package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.repository.PatternRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Сервис получения схемы по id.
 *
 * Эталон — [GetProjectByIdService]. Параметр — VO [PatternId] (D1), не `Long`.
 */
class GetPatternByIdService @Inject constructor(
    private val patternRepository: PatternRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<PatternId, Pattern>(dispatcher) {

    override suspend fun execute(params: PatternId): Result<Pattern> =
        patternRepository.getPatternById(params)
}
