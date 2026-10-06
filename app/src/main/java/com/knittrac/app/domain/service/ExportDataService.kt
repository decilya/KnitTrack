package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * UseCase для экспорта всех данных приложения в JSON-строку.
 */
class ExportDataService @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<String> {
        return syncRepository.exportToJson()
    }
}
