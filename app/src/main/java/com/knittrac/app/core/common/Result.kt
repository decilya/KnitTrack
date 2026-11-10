package com.knittrac.app.core.common

/**
 * Универсальный sealed-класс для типобезопасной обработки результатов операций.
 *
 * Используется для оборачивания успешных результатов и ошибок, что позволяет
 * явно обрабатывать оба случая в вызывающем коде без исключений.
 *
 * Комбинаторы (`map`, `flatMap`, `onSuccess`, `onError`, `getOrNull`,
 * `getOrDefault`, `errorOrNull`) убирают дублирование `when (result) { ... }`
 * в UseCase/ViewModel.
 *
 * @param T Тип данных, возвращаемых при успешном выполнении операции.
 */
sealed class Result<out T> {
    /**
     * Успешное завершение операции.
     *
     * @param data Данные, полученные в результате выполнения операции.
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Ошибка выполнения операции.
     *
     * @param error Объект доменной ошибки [AppError], содержащий информацию о проблеме.
     */
    data class Error(val error: AppError) : Result<Nothing>()

    /**
     * Применяет [transform] к успешному значению, пробрасывая ошибку без изменений.
     *
     * @param transform Функция преобразования успешного значения.
     * @return [Success] с преобразованным значением или исходный [Error].
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Применяет [transform] к успешному значению, возвращая новый [Result].
     * Используется для цепочек операций, которые сами могут вернуть ошибку.
     *
     * @param transform Функция, возвращающая новый [Result].
     * @return Результат [transform] или исходный [Error].
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }

    /**
     * Выполняет [action] при успехе, возвращая исходный результат.
     * Побочный эффект (например, логирование), не меняет результат.
     *
     * @param action Действие над успешным значением.
     * @return Исходный [Result].
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Выполняет [action] при ошибке, возвращая исходный результат.
     * Побочный эффект (например, логирование), не меняет результат.
     *
     * @param action Действие над ошибкой.
     * @return Исходный [Result].
     */
    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }

    /**
     * Возвращает значение при успехе или `null` при ошибке.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Возвращает значение при успехе или [default] при ошибке.
     *
     * @param default Значение по умолчанию.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }

    /**
     * Возвращает ошибку при неудаче или `null` при успехе.
     */
    fun errorOrNull(): AppError? = when (this) {
        is Success -> null
        is Error -> error
    }
}
