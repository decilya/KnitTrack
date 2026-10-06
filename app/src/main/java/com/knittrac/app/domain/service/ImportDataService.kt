package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * UseCase для импорта данных из JSON-строки.
 */
class ImportDataService @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(params: ImportDataParams): Result<Unit> {
        return syncRepository.importFromJson(params.json)
    }
}
