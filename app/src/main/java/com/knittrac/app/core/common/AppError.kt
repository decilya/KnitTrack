package com.knittrac.app.core.common

/**
 * Базовый sealed-класс для всех доменных ошибок приложения.
 *
 * Предоставляет типобезопасный способ представления различных типов ошибок,
 * которые могут возникнуть в приложении. Каждый подкласс представляет
 * конкретный тип ошибки для более точной обработки.
 *
 * @param message Человекочитаемое сообщение об ошибке на русском языке.
 * @param cause Исходное исключение (если есть) — для логирования стектрейса
 *              через Timber. У ValidationError всегда null, у остальных —
 *              тот Throwable, который обернули.
 *
 * @see ValidationError Ошибка валидации входных данных.
 * @see DatabaseError Ошибка при работе с базой данных.
 * @see NetworkError Ошибка сетевого взаимодействия.
 * @see UnknownError Неизвестная ошибка.
 */
sealed class AppError(
    open val message: String,
    val cause: Throwable? = null
) {
    /**
     * Ошибка валидации входных данных.
     *
     * Возникает, когда переданные параметры не соответствуют требованиям
     * бизнес-логики (например, пустое название проекта или отрицательная длительность).
     *
     * @param message Описание ошибки валидации.
     */
    data class ValidationError(override val message: String) : AppError(message)

    /**
     * Ошибка при работе с базой данных Room.
     *
     * Возникает при проблемах с чтением/записью в SQLite базу данных,
     * нарушениях целостности данных или проблемах с миграцией схемы.
     *
     * @param exception Исключение, вызвавшее ошибку (для отладки).
     */
    data class DatabaseError(val exception: Throwable) : AppError(
        exception.message ?: "Неизвестная ошибка базы данных",
        exception
    )

    /**
     * Ошибка сетевого взаимодействия.
     *
     * Возникает при проблемах с подключением к интернету, таймаутах
     * запросов или ошибках сервера.
     *
     * @param exception Исключение, вызвавшее ошибку (для отладки).
     */
    data class NetworkError(val exception: Throwable) : AppError(
        exception.message ?: "Ошибка сетевого подключения",
        exception
    )

    /**
     * Неизвестная ошибка, не подпадающая под другие категории.
     *
     * Используется как запасной вариант для необработанных исключений.
     *
     * @param exception Исключение, вызвавшее ошибку (для отладки).
     */
    data class UnknownError(val exception: Throwable) : AppError(
        exception.message ?: "Неизвестная ошибка",
        exception
    )
}