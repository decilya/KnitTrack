package com.knittrac.app.data.repository

import com.knittrac.app.core.common.AppError
import com.knittrac.app.core.common.Result
import com.knittrac.app.data.local.PatternDao
import com.knittrac.app.data.mapper.toDomain
import com.knittrac.app.domain.entity.Pattern
import com.knittrac.app.domain.entity.PatternId
import com.knittrac.app.domain.repository.PatternRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

class PatternRepositoryImpl @Inject constructor(
    private val patternDao: PatternDao
) : PatternRepository {

    override suspend fun getPatternById(id: PatternId): Result<Pattern> = try {
        val entity = patternDao.getPatternById(id.value)
        if (entity != null) {
            val rows = patternDao.getRowsForPattern(id.value)
            Result.Success(entity.toDomain(rows))
        } else {
            Result.Error(AppError.NotFoundError("Схема id=${id.value} не найдена"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "getPatternById id=%d failed", id.value)
        Result.Error(AppError.DatabaseError(e))
    }
}
