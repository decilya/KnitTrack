package com.knittrack.app.domain.service

import com.knittrack.app.core.base.BaseUseCase
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.di.IoDispatcher
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.repository.PdfPatternExporter
import kotlinx.coroutines.CoroutineDispatcher
import java.io.OutputStream
import javax.inject.Inject

/**
 * Параметры экспорта схемы в PDF.
 *
 * @property pattern Схема для отрисовки.
 * @property outputStream Поток для записи PDF. Не закрывается здесь.
 */
data class ExportPatternPdfParams(
    val pattern: Pattern,
    val outputStream: OutputStream
)

/**
 * UseCase экспорта схемы в PDF.
 */
class ExportPatternPdfUseCase @Inject constructor(
    private val pdfExporter: PdfPatternExporter,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<ExportPatternPdfParams, Unit>(dispatcher) {

    override suspend fun execute(params: ExportPatternPdfParams): Result<Unit> {
        return pdfExporter.exportToPdf(params.pattern, params.outputStream)
    }
}
