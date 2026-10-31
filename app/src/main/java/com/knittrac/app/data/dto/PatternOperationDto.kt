package com.knittrac.app.data.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/** DTO операции ряда. Символы — по коду, не вложенным объектом. */
@Keep
data class PatternOperationDto(
    @SerializedName("patternRowId")
    val patternRowId: Long,
    @SerializedName("columnStart")
    val columnStart: Int,
    @SerializedName("symbolCode")
    val symbolCode: String,
    @SerializedName("backSymbolCode")
    val backSymbolCode: String? = null,
    @SerializedName("colorHex")
    val colorHex: String? = null,
    @SerializedName("columnSpanOverride")
    val columnSpanOverride: Int? = null,
    @SerializedName("rowSpanOverride")
    val rowSpanOverride: Int? = null,
    @SerializedName("repeatCode")
    val repeatCode: String? = null
)
