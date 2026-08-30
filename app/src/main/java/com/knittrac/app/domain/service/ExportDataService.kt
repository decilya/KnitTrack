package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Сервис для экспорта всех данных приложения в формат JSON.
 *
 * Этот UseCase используется для резервного копирования данных.
 * Собирает все проекты и сессии и сериализует их в JSON строку.
 *
 * @param syncRepository Репозиторий для операций синхронизации.
 * @param dispatcher Диспетчер для выполнения фоновой работы.
 *
 * @see SyncRepository.exportToJson Метод экспорта.
 */
class ExportDataService @Inject constructor(
    private val syncRepository: SyncRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<Unit, String>(dispatcher) {

    /**
     * Выполняет экспорт данных в JSON.
     *
     * @param params Пустой параметр (Unit).
     * @return [Result.Success] с JSON строкой, содержащей все данные,
     *         или [Result.Error] при ошибке.
     */
    override suspend fun execute(params: Unit): Result<String> =
        syncRepository.exportToJson()
}