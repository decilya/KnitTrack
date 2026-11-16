package com.knittrack.app.presentation.feature_pattern_editor

import com.knittrack.app.core.common.Result
import com.knittrack.app.domain.entity.Pattern
import com.knittrack.app.domain.service.ExportPatternPdfParams
import com.knittrack.app.domain.service.ExportPatternSvgParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.OutputStream

/**
 * Логика экспорта схемы (SVG и PDF).
 * Вынесена из ViewModel для соблюдения SRP и сокращения размера класса.
 */

internal fun exportPatternSvg(
    pattern: Pattern,
    outputStream: OutputStream,
    useCases: PatternEditorPatternUseCases,
    effectChannel: Channel<PatternEditorContract.Effect>,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            val params = ExportPatternSvgParams(pattern, outputStream)
            when (useCases.exportSvg(params)) {
                is Result.Success -> effectChannel.send(PatternEditorContract.Effect.ExportSuccess)
                is Result.Error -> effectChannel.send(PatternEditorContract.Effect.ExportError)
            }
        } finally {
            try { outputStream.close() } catch (e: Exception) { /* ignore */ }
        }
    }
}

internal fun exportPatternPdf(
    pattern: Pattern,
    outputStream: OutputStream,
    useCases: PatternEditorPatternUseCases,
    effectChannel: Channel<PatternEditorContract.Effect>,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            val params = ExportPatternPdfParams(pattern, outputStream)
            when (useCases.exportPdf(params)) {
                is Result.Success -> effectChannel.send(PatternEditorContract.Effect.ExportSuccess)
                is Result.Error -> effectChannel.send(PatternEditorContract.Effect.ExportError)
            }
        } finally {
            try { outputStream.close() } catch (e: Exception) { /* ignore */ }
        }
    }
}
