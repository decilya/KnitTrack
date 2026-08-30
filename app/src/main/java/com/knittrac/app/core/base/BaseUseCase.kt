package com.knittrac.app.core.base

import com.knittrac.app.core.common.AppErrorMapper
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Базовый абстрактный класс для всех UseCase, возвращающих единичный результат.
 *
 * Этот класс реализует общий шаблон для бизнес-операций (UseCase) в приложении:
 * 1. Принимает параметры типа [Params]
 * 2. Выполняет операцию на фоновом диспетчере ([IoDispatcher])
 * 3. Возвращает результат, обернутый в [Result]
 * 4. Автоматически перехватывает и преобразует исключения в [AppError]
 *
 * Использование этого базового класса обеспечивает:
 * - Единый подход к обработке ошибок во всех UseCase
 * - Автоматическое переключение на фоновый поток
 * - Типобезопасность через дженерики
 *
 * @param Params Тип входных параметров (должен быть contravariant — `in`).
 * @param T Тип успешного результата (должен быть covariant — `out`).
 * @param dispatcher Диспетчер корутин для выполнения фоновой работы (помечен [IoDispatcher]).
 *
 * @see BaseFlowUseCase Для UseCase, возвращающих поток данных (Flow).
 */
abstract class BaseUseCase<in Params, out T>(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * Основной метод, в котором реализуется бизнес-логика UseCase.
     *
     * Этот метод должен быть переопределен в каждом конкретном UseCase
     * для реализации конкретной бизнес-операции. Метод выполняется
     * в контексте, предоставленном [dispatcher] (по умолчанию — IO-диспетчер).
     *
     * @param params Входные параметры для выполнения операции.
     * @return Результат операции в виде [Result], содержащий либо успешные данные,
     *         либо ошибку.
     *
     * @see invoke Оператор вызова UseCase.
     */
    protected abstract suspend fun execute(params: Params): Result<T>

    /**
     * Оператор вызова (invoke). Позволяет использовать экземпляр UseCase как функцию.
     *
     * Этот метод является публичным API для вызова UseCase. Он:
     * 1. Переключает контекст выполнения на фоновый диспетчер ([dispatcher])
     * 2. Вызывает метод [execute] для выполнения бизнес-логики
     * 3. Перехватывает любые непредвиденные исключения и преобразует их
     *    в [Result.Error] через [AppErrorMapper]
     *
     * Пример использования:
     * ```kotlin
     * val result = addProjectService(AddProjectParams("Шарф", Category.KNITTING))
     * when (result) {
     *     is Result.Success -> println("Project ID: ${result.data}")
     *     is Result.Error -> println("Error: ${result.error.message}")
     * }
     * ```
     *
     * @param params Параметры для выполнения.
     * @return Результат выполнения, обёрнутый в [Result].
     */
    suspend operator fun invoke(params: Params): Result<T> = withContext(dispatcher) {
        try {
            // Выполняем основную бизнес-логику
            execute(params)
        } catch (e: Exception) {
            // Перехватываем любые непредвиденные исключения и преобразуем
            // их в доменную ошибку для единообразной обработки
            Result.Error(AppErrorMapper.map(e))
        }
    }
}