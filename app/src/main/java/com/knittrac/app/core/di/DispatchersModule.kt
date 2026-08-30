package com.knittrac.app.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Квалификатор для внедрения диспетчера ввода-вывода (IO).
 *
 * Используется для маркировки [CoroutineDispatcher], который предназначен
 * для выполнения фоновых операций с интенсивным вводом-выводом:
 * - Чтение/запись в базу данных Room
 * - Сетевые запросы
 * - Работа с файловой системой
 *
 * @see Dispatchers.IO Стандартный диспетчер для IO-операций.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Квалификатор для внедрения диспетчера главного потока (Main).
 *
 * Используется для маркировки [CoroutineDispatcher], который предназначен
 * для выполнения операций в главном потоке приложения:
 * - Обновление UI
 * - Взаимодействие с Android-компонентами
 *
 * @see Dispatchers.Main Стандартный диспетчер для главного потока.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * DI-модуль для предоставления диспетчеров корутин через Hilt.
 *
 * Этот модуль регистрирует в графе зависимостей Hilt два основных
 * диспетчера корутин с соответствующими квалификаторами, что позволяет
 * внедрять их в UseCase и другие компоненты без жесткой привязки
 * к конкретным реализациям.
 *
 * @see IoDispatcher Квалификатор для IO-диспетчера.
 * @see MainDispatcher Квалификатор для Main-диспетчера.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    /**
     * Предоставляет диспетчер ввода-вывода для фоновых операций.
     *
     * Этот диспетчер оптимизирован для выполнения операций с интенсивным
     * вводом-выводом и использует пул потоков, размер которого зависит
     * от количества доступных процессоров.
     *
     * @return [CoroutineDispatcher] для IO-операций ([Dispatchers.IO]).
     */
    @Provides
    @IoDispatcher
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Предоставляет диспетчер главного потока для UI-операций.
     *
     * Этот диспетчер выполняет корутины в главном потоке приложения,
     * что необходимо для обновления UI и взаимодействия с Android-компонентами.
     *
     * @return [CoroutineDispatcher] для главного потока ([Dispatchers.Main]).
     */
    @Provides
    @MainDispatcher
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}