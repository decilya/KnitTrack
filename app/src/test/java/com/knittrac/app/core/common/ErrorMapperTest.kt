package com.knittrac.app.core.common

import com.knittrac.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты [ErrorMapper].
 *
 * Гарантируют, что для каждого [ErrorCode] (кроме [ErrorCode.UNKNOWN_ERROR],
 * который по контракту мапится в fallback) есть собственный строковый ресурс.
 * Fallback `error_unknown` в [ErrorMapper.map] — защитная сетка; тест ловит
 * момент, когда новый [ErrorCode] добавили, а в маппинг забыли.
 */
class ErrorMapperTest {

    /**
     * Проверяет, что для каждого [ErrorCode], кроме [ErrorCode.UNKNOWN_ERROR],
     * [ErrorMapper.map] возвращает не-fallback ресурс.
     */
    @Test
    fun everyErrorCodeExceptUnknownHasDedicatedStringRes() {
        val fallback = R.string.error_unknown
        val unmapped = ErrorCode.entries
            .filter { it != ErrorCode.UNKNOWN_ERROR }
            .filter { ErrorMapper.map(it) == fallback }
        assertTrue(
            "ErrorCode без собственного R.string (мапится в error_unknown): $unmapped",
            unmapped.isEmpty(),
        )
    }

    /**
     * Проверяет, что [ErrorCode.UNKNOWN_ERROR] по контракту мапится в fallback `error_unknown`.
     */
    @Test
    fun unknownErrorMapsToFallbackStringRes() {
        assertEquals(R.string.error_unknown, ErrorMapper.map(ErrorCode.UNKNOWN_ERROR))
    }
}
