package com.knittrac.app.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль для предоставления зависимостей слоя данных (Data Layer).
 * 
 * Обеспечивает DI-граф (Dependency Injection Graph) базовыми объектами, 
 * которые требуются репозиториям и сервисам, но не могут быть созданы автоматически 
 * (например, сторонние библиотеки вроде Gson или системный Context).
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /**
     * Предоставляет singleton-экземпляр [Gson] для сериализации/десериализации JSON.
     * Использование одного экземпляра (Singleton) экономит память и улучшает производительность,
     * так как Gson не хранит состояния и полностью потокобезопасен.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    /**
     * Предоставляет [Context] приложения для репозиториев.
     * 
     * ВАЖНО: Мы используем @ApplicationContext, чтобы гарантировать, что в Singleton-репозитории
     * не произойдет утечки памяти (если бы мы передали Activity Context, он бы удерживался 
     * в памяти навсегда, что привело бы к краху приложения).
     */
    @Provides
    fun provideContext(@ApplicationContext context: Context): Context = context
}
