package com.knittrac.app.domain.service

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.repository.DataExporter
import java.io.OutputStream
import javax.inject.Inject

/**
 * UseCase для экспорта всех данных в предоставленный поток.
 *
 * Делегирует работу в [DataExporter], соблюдая DIP.
 * Поток не закрывает — ответственность вызывающей стороны.
 */
class ExportDataService @Inject constructor(
    private val dataExporter: DataExporter
) {
    /**
     * @param outputStream поток для записи (не закрывается здесь).
     * @return [Result.Success] при успехе, [Result.Error] при сбое.
     */
    suspend operator fun invoke(outputStream: OutputStream): Result<Unit> =
        dataExporter.exportTo(outputStream)
}
