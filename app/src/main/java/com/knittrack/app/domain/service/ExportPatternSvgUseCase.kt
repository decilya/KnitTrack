package com.knittrack.app.domain.service

import com.knittrack.app.core.base.BaseUseCase
import com.knittrack.app.core.common.AppError
import com.knittrack.app.core.common.Result
import com.knittrack.app.core.di.IoDispatcher
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.repository.PatternImageExporter
import kotlinx.coroutines.CoroutineDispatcher
import java.io.OutputStream
import javax.inject.Inject

/**
 * Параметры экспорта схемы в SVG.
 *
 * @property pattern Схема для отрисовки.
 * @property outputStream Поток для записи SVG-строки. Не закрывается здесь —
 *   это ответственность вызывающей стороны (Presentation через SAF).
 */
data class ExportPatternSvgParams(
    val pattern: Pattern,
    val outputStream: OutputStream
)

/**
 * UseCase экспорта схемы в SVG-файл (7f-B-5).
 *
 * Делегирует генерацию SVG в [PatternImageExporter] (DIP: Domain зависит от
 * абстракции, а не от конкретного рендерера в Data-слое). Запись строки в поток
 * выполняется на IO-диспетчере через [BaseUseCase].
 *
 * Поток НЕ закрывается: его жизненным циклом управляет Screen (SAF-лаунчер),
 * чтобы избежать утечки файловых дескрипторов и двойного закрытия.
 */
class ExportPatternSvgUseCase @Inject constructor(
    private val imageExporter: PatternImageExporter,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<ExportPatternSvgParams, Unit>(dispatcher) {

    override suspend fun execute(params: ExportPatternSvgParams): Result<Unit> {
        return imageExporter.exportToSvg(params.pattern)
            .flatMap { svg -> writeToStream(svg, params.outputStream) }
    }

    /**
     * Записывает SVG-строку в поток и закрывает writer (но не сам поток).
     *
     * @return [Result.Success] при успехе, [Result.Error] при сбое записи.
     */
    private fun writeToStream(svg: String, outputStream: OutputStream): Result<Unit> {
        return try {
            outputStream.bufferedWriter().use { it.write(svg) }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.IoError(e))
        }
    }
}