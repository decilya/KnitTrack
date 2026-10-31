package com.knittrac.app.data.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/** DTO символа схемы. `ownerPatternId = null` — системный символ. */
@Keep
data class SymbolDto(
    @SerializedName("code")
    val code: String,
    @SerializedName("ownerPatternId")
    val ownerPatternId: Long? = null,
    @SerializedName("displayName")
    val displayName: String,
    @SerializedName("textGlyph")
    val textGlyph: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("defaultColumnSpan")
    val defaultColumnSpan: Int = 1,
    @SerializedName("defaultRowSpan")
    val defaultRowSpan: Int = 1
)
