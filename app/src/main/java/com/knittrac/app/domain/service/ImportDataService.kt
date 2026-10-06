package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.DataImporter
import java.io.InputStream
import javax.inject.Inject

/**
 * UseCase для импорта всех данных из предоставленного потока.
 *
 * Делегирует работу в [DataImporter], соблюдая DIP.
 * Поток не закрывает — ответственность вызывающей стороны.
 */
class ImportDataService @Inject constructor(
    private val dataImporter: DataImporter
) {
    /**
     * @param inputStream поток для чтения (не закрывается здесь).
     * @return [Result.Success] при успехе, [Result.Error] при сбое.
     */
    suspend operator fun invoke(inputStream: InputStream): Result<Unit> =
        dataImporter.importFrom(inputStream)
}
