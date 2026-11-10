package com.knittrac.app.core.common


/**
 * Код доменной ошибки.
 *
 * Domain и Data возвращают только [ErrorCode], без текста. Текст для пользователя
 * выбирается в Presentation через `ErrorMapper` (ресурсы `@StringRes`).
 */
enum class ErrorCode {
    // Validation
    INVALID_BACKUP_OPERATIONS,
    STITCH_WITHOUT_ROW,
    PATTERN_NOT_ATTACHED,
    REPEAT_NOT_FOUND_IN_SCHEMA,
    SYMBOL_ALREADY_EXISTS,
    INVALID_ROW_COUNT,
    SYMBOL_SYSTEM_DELETE_FORBIDDEN,
    SYMBOL_IN_USE,
    PATTERN_ROW_NOT_FOUND_IN_SCHEMA,
    SCHEMA_EMPTY,
    CURRENT_ROW_LAST_IN_SCHEMA,
    ROW_NOT_IN_SCHEMA,
    EMPTY_SESSION,
    SYMBOL_NOT_IN_LEGEND,
    PATTERN_OVERLAP,
    ROW_NUMBER_OUT_OF_BOUNDS,
    UNSUPPORTED_LANGUAGE,
    INVALID_ARGUMENT,

    // NotFound
    PROJECT_NOT_FOUND,
    PATTERN_NOT_FOUND,
    SYMBOL_NOT_FOUND,
    ROW_NOT_FOUND,
    SESSION_NOT_FOUND,
    OPERATION_NOT_FOUND,

    // Infrastructure
    DATABASE_ERROR,
    IO_ERROR,
    NETWORK_ERROR,
    UNKNOWN_ERROR
}
