package com.knittrac.app.core.common


/**
 * Базовая доменная ошибка.
 *
 * Содержит только [code] (для UI-маппинга) и технические детали [cause]/[details]
 * (для логов). Текст для пользователя берётся из `@StringRes` в Presentation.
 */
sealed class AppError(
    open val code: ErrorCode,
    val cause: Throwable? = null,
    open val details: String? = null
) {
    /** Ошибка валидации входных данных. */
    data class ValidationError(
        override val code: ErrorCode,
        override val details: String? = null
    ) : AppError(code, details = details)

    /** Сущность не найдена. */
    data class NotFoundError(
        override val code: ErrorCode,
        override val details: String? = null
    ) : AppError(code, details = details)

    /** Ошибка БД. */
    data class DatabaseError(val exception: Throwable) : AppError(
        ErrorCode.DATABASE_ERROR,
        cause = exception,
        details = exception.message
    )

    /** Ошибка ввода-вывода. */
    data class IoError(val exception: Throwable) : AppError(
        ErrorCode.IO_ERROR,
        cause = exception,
        details = exception.message
    )

    /** Неизвестная ошибка. */
    data class UnknownError(val exception: Throwable) : AppError(
        ErrorCode.UNKNOWN_ERROR,
        cause = exception,
        details = exception.message
    )

    /** Сетевая ошибка (резерв, Фаза 3). */
    @Deprecated("Будет использоваться на Фазе 3 для сетевых ошибок (Firebase)")
    data class NetworkError(val exception: Throwable) : AppError(
        ErrorCode.NETWORK_ERROR,
        cause = exception,
        details = exception.message
    )
}
