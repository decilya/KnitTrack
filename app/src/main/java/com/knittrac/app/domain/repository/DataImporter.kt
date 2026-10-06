package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import java.io.InputStream

/**
 * Контракт для импорта всех данных приложения.
 *
 * Единственная ответственность (SRP): прочитать данные из предоставленного потока.
 * Реализация не знает, откуда поток пришёл (файл, сеть, память).
 */
interface DataImporter {
    /**
     * Импортирует проекты и сессии из [inputStream] потоково.
     *
     * ВАЖНО: реализация НЕ закрывает поток — это ответственность вызывающей стороны.
     *
     * @param inputStream поток для чтения данных.
     * @return [Result.Success] при успехе, [Result.Error] при сбое I/O или парсинга.
     */
    suspend fun importFrom(inputStream: InputStream): Result<Unit>
}
