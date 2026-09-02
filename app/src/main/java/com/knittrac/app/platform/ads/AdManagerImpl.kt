package com.knittrac.app.platform.ads

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Заглушка менеджера рекламы.
 * Будет реализован на поздних этапах.
 */
@Singleton
class AdManagerImpl @Inject constructor(
    private val context: Context
) {
    // Временные заглушки методов
    fun initialize() { /* TODO */ }
    fun loadBannerAd() { /* TODO */ }
}