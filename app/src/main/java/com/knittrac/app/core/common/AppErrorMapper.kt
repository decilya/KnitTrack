package com.knittrac.app.core.common

import java.io.IOException

/**
 * Утилита для маппинга стандартных исключений Java/Kotlin в доменные ошибки [AppError].
 * 
 * Этот объект предоставляет централизованный способ преобразования низкоуровневых
 * исключений (IOException, SQLException и т.д.) в высокоуровневые доменные ошибки,
 * которые понятны бизнес-логике приложения.
 * 
 * @see AppError Доменный класс ошибок.
 */
object AppErrorMapper {
    /**
     * Преобразует исключение в соответствующий тип доменной ошибки [AppError].
     * 
     * Метод анализирует тип переданного исключения и возвращает наиболее
     * подходящий подкласс [AppError]:
     * - [IOException] → [AppError.NetworkError]
     * - [IllegalArgumentException] → [AppError.ValidationError]
     * - Все остальные → [AppError.UnknownError]
     * 
     * @param e Исключение, которое необходимо преобразовать.
     * @return Экземпляр [AppError], соответствующий типу исключения.
     */
    fun map(e: Throwable): AppError = when (e) {
        // Ошибки ввода-вывода обычно связаны с сетевыми проблемами
        is IOException -> AppError.NetworkError(e)

        // Ошибки аргументов — это ошибки валидации данных
        is IllegalArgumentException -> AppError.ValidationError(
            e.message ?: "Некорректные входные данные"
        )

        // Все остальные исключения считаем неизвестными ошибками
        else -> AppError.UnknownError(e)
    }
}