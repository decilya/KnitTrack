package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseTest
import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ColumnStart
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.entity.PatternOperation
import com.knittrac.app.domain.entity.PatternRow
import com.knittrac.app.domain.entity.PatternRowId
import com.knittrac.app.domain.entity.RowNumber
import com.knittrac.app.domain.entity.Span
import com.knittrac.app.domain.entity.Symbol
import com.knittrac.app.domain.entity.SymbolCategory
import com.knittrac.app.domain.model.AddPatternOperationParams
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [AddPatternOperationUseCase].
 *
 * Проверяют тонкий делегирующий слой: проброс [AddPatternOperationParams]
 * в [AddPatternOperationService], возврат созданной [PatternOperation]
 * или ошибки без дополнительной логики.
 */
class AddPatternOperationUseCaseTest : BaseTest() {

    private val service: AddPatternOperationService = mockk()
    private val useCase = AddPatternOperationUseCase(service, coroutineRule.testDispatcher)

    private fun symbol(): Symbol = Symbol(
        code = "K",
        displayName = "Лицевая",
        textGlyph = "V",
        category = SymbolCategory.STANDARD,
        defaultColumnSpan = Span.ONE
    )

    private fun params(): AddPatternOperationParams = AddPatternOperationParams(
        patternId = PatternId.create(1L),
        row = PatternRow(
            id = PatternRowId.create(10L),
            rowNumber = RowNumber.create(1)
        ),
        columnStart = ColumnStart.create(0),
        symbol = symbol()
    )

    /**
     * Успешный путь: сервис вернул созданную операцию — пробрасываем как Success.
     */
    @Test
    fun `execute delegates to service and returns operation`() = runTest {
        val op = PatternOperation(
            patternRowId = PatternRowId.create(10L),
            columnStart = ColumnStart.create(0),
            symbol = symbol()
        )
        coEvery { service.add(any()) } returns Result.Success(op)

        val result = useCase(params())

        assertTrue(result is Result.Success)
        assertEquals(op, (result as Result.Success).data)
        coVerify { service.add(any()) }
    }

    /**
     * Валидационная ошибка сервиса (символ не в легенде) — пробрасываем.
     */
    @Test
    fun `execute returns validation error from service`() = runTest {
        coEvery { service.add(any()) } returns
            Result.Error(AppError.ValidationError("нет в легенде"))

        val result = useCase(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ValidationError)
    }

    /**
     * Ошибка БД сервиса — пробрасываем.
     */
    @Test
    fun `execute returns database error from service`() = runTest {
        coEvery { service.add(any()) } returns
            Result.Error(AppError.DatabaseError(RuntimeException("db")))

        val result = useCase(params())

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.DatabaseError)
    }
}
