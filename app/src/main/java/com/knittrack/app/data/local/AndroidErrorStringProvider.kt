package com.knittrack.app.data.local

import androidx.annotation.StringRes
import com.knittrack.app.core.common.ErrorCode
import com.knittrack.app.core.common.ErrorMapper
import com.knittrack.app.core.common.ErrorStringProvider
import javax.inject.Inject

/** Реализация [ErrorStringProvider] на базе Android `R.string` (через [ErrorMapper]). */
class AndroidErrorStringProvider @Inject constructor() : ErrorStringProvider {
    @StringRes
    override fun getStringRes(code: ErrorCode): Int = ErrorMapper.map(code)
}
