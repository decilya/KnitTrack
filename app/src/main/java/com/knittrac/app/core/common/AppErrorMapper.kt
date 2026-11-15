package com.knittrac.app.core.common


import android.database.SQLException
import java.io.IOException

/**
 * Маппинг исключений в [AppError].
 */
object AppErrorMapper {

    /**
     * Преобразует [e] в [AppError] по типу исключения.
     *
     * @param e Throwable (исключение или ошибка).
     * @param fallbackCode Код для [IllegalArgumentException] (по умолчанию `INVALID_ARGUMENT`).
     */
    fun map(e: Throwable, fallbackCode: ErrorCode = ErrorCode.INVALID_ARGUMENT): AppError = when (e) {
        is IOException -> AppError.IoError(e)
        is IllegalArgumentException -> AppError.ValidationError(fallbackCode, e.message)
        is SQLException -> AppError.DatabaseError(e)
        else -> AppError.UnknownError(e)
    }
}
