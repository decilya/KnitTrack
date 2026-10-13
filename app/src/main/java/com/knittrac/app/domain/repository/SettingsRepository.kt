package com.knittrac.app.domain.repository

import com.knittrac.app.core.common.Result
import com.knittrac.app.domain.entity.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Контракт репозитория пользовательских настроек.
 *
 * Чистый интерфейс без зависимостей от Android/DataStore: Domain
 * описывает, что нужно, Data — как это хранится (DataStore Preferences).
 * Тема представлена типобезопасным enum [ThemeMode].
 */
interface SettingsRepository {

    /** Реактивный поток текущего режима темы. */
    val themeModeFlow: Flow<ThemeMode>

    /** Реактивный поток флага включённых уведомлений. */
    val notificationsEnabledFlow: Flow<Boolean>

    /**
     * Сохраняет выбранный режим темы.
     *
     * @param mode Новый режим.
     * @return [Result.Success] или [Result.Error] с [AppError.IoError]
     *   при сбое DataStore, либо [AppError.UnknownError].
     */
    suspend fun setThemeMode(mode: ThemeMode): Result<Unit>

    /**
     * Сохраняет флаг уведомлений.
     *
     * @param enabled true — уведомления включены.
     * @return [Result.Success] или [Result.Error].
     */
    suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit>
}
