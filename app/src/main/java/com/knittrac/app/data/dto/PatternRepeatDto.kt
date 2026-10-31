package com.knittrac.app.data.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/** DTO раппорта. `patternId` есть в JSON: в Domain его нет. */
@Keep
data class PatternRepeatDto(
    @SerializedName("patternId")
    val patternId: Long,
    @SerializedName("code")
    val code: String,
    @SerializedName("stitchCount")
    val stitchCount: Int,
    @SerializedName("rowCount")
    val rowCount: Int
)
