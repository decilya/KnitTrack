package com.knittrac.app.domain.service

import com.knittrac.app.core.base.BaseUseCase
import com.knittrac.app.core.common.Result
import com.knittrac.app.core.di.IoDispatcher
import com.knittrac.app.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Сервис для импорта данных приложения из JSON.
 *
 * Этот UseCase используется для восстановления данных из резервной копии.
 * На Этапе 1 реализована базовая заглушка без разрешения конфликтов.
 *
 * @param syncRepository Репозиторий для операций синхронизации.
 * @param dispatcher Диспетчер для выполнения фоновой работы.
 *
 * @see SyncRepository.importFromJson Метод импорта.
 */
class ImportDataService @Inject constructor(
    private val syncRepository: SyncRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher
) : BaseUseCase<ImportDataParams, Unit>(dispatcher) {

    /**
     * Выполняет импорт данных из JSON.
     *
     * @param params Параметры импорта (JSON строка).
     * @return [Result.Success] при успешном импорте или [Result.Error] при ошибке.
     */
    override suspend fun execute(params: ImportDataParams): Result<Unit> =
        syncRepository.importFromJson(params.json)
}