package com.knittrac.app.data.local

import androidx.annotation.StringRes
import com.knittrac.app.core.common.ErrorCode
import com.knittrac.app.core.common.ErrorMapper
import com.knittrac.app.core.common.ErrorStringProvider
import javax.inject.Inject

/** Реализация [ErrorStringProvider] на базе Android `R.string` (через [ErrorMapper]). */
class AndroidErrorStringProvider @Inject constructor() : ErrorStringProvider {
    @StringRes
    override fun getStringRes(code: ErrorCode): Int = ErrorMapper.map(code)
}
