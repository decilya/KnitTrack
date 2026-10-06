package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import java.io.OutputStream

/**
 * Контракт для экспорта всех данных приложения.
 *
 * Единственная ответственность (SRP): записать данные в предоставленный поток.
 * Реализация не знает, куда поток ведёт (файл, сеть, память) — это задача вызывающей стороны.
 */
interface DataExporter {
    /**
     * Экспортирует все проекты и сессии в [outputStream] потоково.
     *
     * ВАЖНО: реализация НЕ закрывает поток — это ответственность вызывающей стороны
     * (обычно явный close() в UI-слое после получения Effect).
     *
     * @param outputStream поток для записи данных.
     * @return [Result.Success] при успехе, [Result.Error] при сбое I/O.
     */
    suspend fun exportTo(outputStream: OutputStream): Result<Unit>
}
