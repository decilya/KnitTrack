package com.knittrac.app.domain.repository

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
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Сохраняет флаг уведомлений.
     *
     * @param enabled true — уведомления включены.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
