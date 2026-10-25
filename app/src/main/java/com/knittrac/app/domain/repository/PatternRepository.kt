package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId

interface PatternRepository {
    suspend fun getPatternById(id: PatternId): Result<Pattern>
}
