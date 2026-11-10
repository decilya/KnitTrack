package com.knittrac.app.core.common


import androidx.annotation.StringRes
import com.knittrac.app.R

/**
 * Маппинг [ErrorCode] в строковый ресурс (`@StringRes`).
 *
 * Domain/Data возвращают только код. Presentation использует этот маппер
 * для отображения локализованного текста.
 */
object ErrorMapper {

    private val resourceMap = mapOf(
        ErrorCode.INVALID_BACKUP_OPERATIONS to R.string.error_invalid_backup,
        ErrorCode.STITCH_WITHOUT_ROW to R.string.error_stitch_without_row,
        ErrorCode.PATTERN_NOT_ATTACHED to R.string.error_pattern_not_attached,
        ErrorCode.REPEAT_NOT_FOUND_IN_SCHEMA to R.string.error_repeat_not_in_schema,
        ErrorCode.SYMBOL_ALREADY_EXISTS to R.string.error_symbol_already_exists,
        ErrorCode.INVALID_ROW_COUNT to R.string.error_invalid_row_count,
        ErrorCode.SYMBOL_SYSTEM_DELETE_FORBIDDEN to R.string.error_system_symbol_delete,
        ErrorCode.SYMBOL_IN_USE to R.string.error_symbol_in_use,
        ErrorCode.PATTERN_ROW_NOT_FOUND_IN_SCHEMA to R.string.error_row_not_in_schema,
        ErrorCode.SCHEMA_EMPTY to R.string.error_schema_empty,
        ErrorCode.CURRENT_ROW_LAST_IN_SCHEMA to R.string.error_last_row_in_schema,
        ErrorCode.ROW_NOT_IN_SCHEMA to R.string.error_row_not_in_schema,
        ErrorCode.EMPTY_SESSION to R.string.error_empty_session,
        ErrorCode.SYMBOL_NOT_IN_LEGEND to R.string.error_symbol_not_in_legend,
        ErrorCode.PATTERN_OVERLAP to R.string.error_pattern_overlap,
        ErrorCode.ROW_NUMBER_OUT_OF_BOUNDS to R.string.error_row_number_out_of_bounds,
        ErrorCode.UNSUPPORTED_LANGUAGE to R.string.error_unsupported_language,
        ErrorCode.INVALID_ARGUMENT to R.string.error_invalid_argument,
        ErrorCode.PROJECT_NOT_FOUND to R.string.error_project_not_found,
        ErrorCode.PATTERN_NOT_FOUND to R.string.error_pattern_not_found,
        ErrorCode.SYMBOL_NOT_FOUND to R.string.error_symbol_not_found,
        ErrorCode.ROW_NOT_FOUND to R.string.error_row_not_found,
        ErrorCode.SESSION_NOT_FOUND to R.string.error_session_not_found,
        ErrorCode.OPERATION_NOT_FOUND to R.string.error_operation_not_found,
        ErrorCode.DATABASE_ERROR to R.string.error_database,
        ErrorCode.IO_ERROR to R.string.error_io,
        ErrorCode.NETWORK_ERROR to R.string.error_network,
        ErrorCode.UNKNOWN_ERROR to R.string.error_unknown
    )

    /**
     * Возвращает `@StringRes` для [code]. Fallback — `error_unknown`.
     */
    @StringRes
    fun map(code: ErrorCode): Int = resourceMap[code] ?: R.string.error_unknown
}
