package com.knittrac.app.domain.service

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.PatternRepository
import javax.inject.Inject

/**
 * Сервис перемещения ряда (сдвиг) (7f-D-3).
 *
 * Меняет rowNumber ряда, сдвигая соседние ряды вверх или вниз.
 * Использует легковесные запросы к БД для валидации.
 */
class UpdatePatternRowNumberService @Inject constructor(
    private val patternRepository: PatternRepository
) {
    /**
     * Перемещает ряд на новый номер.
     *
     * @param params Параметры перемещения.
     * @return [Result.Success] или [Result.Error] с причиной отказа.
     */
    suspend fun update(params: UpdatePatternRowNumberParams): Result<Unit> =
        when (val existsResult = patternRepository.existsRow(params.patternId, params.rowId)) {
            is Result.Error -> existsResult
            is Result.Success -> {
                if (!existsResult.data) {
                    Result.Error(AppError.NotFoundError("Ряд ${params.rowId.value} не найден в схеме ${params.patternId.value}"))
                } else {
                    validateBoundsAndMove(params)
                }
            }
        }

    /**
     * Проверяет границы и выполняет перемещение.
     */
    private suspend fun validateBoundsAndMove(params: UpdatePatternRowNumberParams): Result<Unit> =
        when (val oldNumberResult = patternRepository.getRowNumber(params.rowId)) {
            is Result.Error -> oldNumberResult
            is Result.Success -> {
                when (val countResult = patternRepository.getRowCount(params.patternId)) {
                    is Result.Error -> countResult
                    is Result.Success -> {
                        val oldNumber = oldNumberResult.data
                        val rowCount = countResult.data
                        when {
                            params.newRowNumber.value > rowCount -> Result.Error(
                                AppError.ValidationError("Новый номер ряда ${params.newRowNumber.value} выходит за пределы (всего $rowCount рядов)")
                            )
                            oldNumber == params.newRowNumber.value -> Result.Success(Unit)
                            else -> patternRepository.updateRowNumber(params.patternId, params.rowId, params.newRowNumber)
                        }
                    }
                }
            }
        }
}
