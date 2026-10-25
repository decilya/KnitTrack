package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.model.AddPatternOperationParams
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * UseCase добавления операции в ряд схемы.
 *
 * Application-слой, тонкая обёртка над [AddPatternOperationService]:
 * вся валидация (символ в легенде, раппорт, overlap) — в Domain-сервисе.
 *
 * @see AddPatternOperationService
 * @see AddPatternOperationParams
 */
class AddPatternOperationUseCase @Inject constructor(
    private val service: AddPatternOperationService,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<AddPatternOperationParams, PatternOperation>(dispatcher) {

    /**
     * Добавляет операцию.
     *
     * @param params Параметры добавления (схема, ряд, колонка, символ).
     * @return [Result.Success] с созданной [PatternOperation];
     *   [Result.Error] с [com.knittrac.app.core.common.AppError.ValidationError]
     *   при нарушении инварианта (символ не в легенде, раппорт не найден, overlap)
     *   или [com.knittrac.app.core.common.AppError.DatabaseError] при сбое БД.
     */
    override suspend fun execute(params: AddPatternOperationParams): Result<PatternOperation> =
        service.add(params)
}
