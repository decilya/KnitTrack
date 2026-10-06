package com.knittrac.app.core.base

import com.knittrac.app.core.common.AppErrorMapper
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Базовый абстрактный класс для UseCase, возвращающих поток данных (Flow).
 *
 * @param Params Тип входных параметров (должен быть contravariant — `in`).
 * @param T Тип элементов потока (должен быть covariant — `out`).
 * @param dispatcher Диспетчер корутин для выполнения фоновой работы.
 */
abstract class BaseFlowUseCase<in Params, out T>(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * Основной метод, возвращающий поток данных.
     */
    protected abstract fun execute(params: Params): Flow<T>

    /**
     * Оператор вызова. Добавляет обработку ошибок в поток и переключает
     * контекст на фоновый диспетчер.
     */
    operator fun invoke(params: Params): Flow<Result<T>> = execute(params)
        // Явное приведение типа ЗДЕСЬ НЕОБХОДИМО, чтобы компилятор Kotlin
        // правильно вывел тип Flow как Flow<Result<T>>, а не Flow<Result.Success<T>>.
        // Без этого блок catch не сможет эмитить Result.Error.
        .map<T, Result<T>> { data -> Result.Success(data) }
        .catch { e -> emit(Result.Error(AppErrorMapper.map(e))) }
        .flowOn(dispatcher)
}