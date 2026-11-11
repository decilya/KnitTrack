package com.knittrac.app.core.common

import androidx.annotation.StringRes

/**
 * Провайдер строковых ресурсов для [ErrorCode].
 * Абстрагирует Presentation от `R.string`, позволяя мокать в тестах.
 */
interface ErrorStringProvider {
    /**
     * Возвращает строковый ресурс (@StringRes) для заданного [code].
     */
    @StringRes
    fun getStringRes(code: ErrorCode): Int
}
