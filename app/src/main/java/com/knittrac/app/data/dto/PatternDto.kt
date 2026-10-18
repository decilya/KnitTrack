package com.knittrac.app.data.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * DTO схемы для сериализации в JSON.
 *
 * Содержит метаданные схемы и калибровку фонового изображения.
 * Операции (петли) вынесены в отдельные таблицы.
 */
@Keep
data class PatternDto(
    /** Уникальный идентификатор схемы. */
    @SerializedName("id")
    val id: Long = 0L,

    /** Название схемы. */
    @SerializedName("name")
    val name: String,

    /** Техника вязания (KNITTING, CROCHET, CROSS_STITCH, BEADING). */
    @SerializedName("technique")
    val technique: String,

    /** Направление чтения схемы (LTR, RTL, ALTERNATING). */
    @SerializedName("direction")
    val direction: String,

    /** Двусторонняя ли схема (ЛС/ИС). */
    @SerializedName("isReversible")
    val isReversible: Boolean,

    /** Путь к фоновому изображению (URI). */
    @SerializedName("backgroundImageUri")
    val backgroundImageUri: String? = null,

    /** Смещение фона по X. */
    @SerializedName("imageOffsetX")
    val imageOffsetX: Float = 0f,

    /** Смещение фона по Y. */
    @SerializedName("imageOffsetY")
    val imageOffsetY: Float = 0f,

    /** Масштаб фона. */
    @SerializedName("imageScale")
    val imageScale: Float = 1f
)
