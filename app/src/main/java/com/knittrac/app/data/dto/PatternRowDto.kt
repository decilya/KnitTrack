package com.knittrac.app.data.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * DTO ряда схемы для сериализации в JSON.
 *
 * Хранит порядковый номер ряда и привязку к схеме.
 */
@Keep
data class PatternRowDto(
    /** Уникальный идентификатор ряда. */
    @SerializedName("id")
    val id: Long = 0L,

    /** Идентификатор схемы. */
    @SerializedName("patternId")
    val patternId: Long,

    /** Номер ряда для отображения. */
    @SerializedName("rowNumber")
    val rowNumber: Int
)
