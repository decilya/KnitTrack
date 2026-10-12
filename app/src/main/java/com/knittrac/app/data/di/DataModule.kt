package com.knittrac.app.data.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль для предоставления зависимостей Data-слоя.
 *
 * Содержит только то, что не может быть создано Hilt автоматически:
 * экземпляры сторонних библиотек (Gson).
 *
 * Context НЕ предоставляется здесь: Hilt уже даёт встроенный биндинг
 * @ApplicationContext Context. Дублирующий @Provides был бы мёртвым кодом.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /**
     * Предоставляет singleton-экземпляр [Gson] для сериализации JSON.
     *
     * Gson потокобезопасен и не хранит состояния — один экземпляр
     * экономит память и исключает пересоздание внутренних структур
     * (TypeAdapterFactory, рефлексия) на каждый вызов.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}
