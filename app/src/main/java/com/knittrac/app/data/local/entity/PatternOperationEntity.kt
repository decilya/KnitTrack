package com.knittrac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Room-сущность операции (петли) в ряду схемы. */
@Entity(
    tableName = "pattern_operations",
    primaryKeys = ["pattern_row_id", "column_start"],
    foreignKeys = [
        ForeignKey(entity = PatternRowEntity::class, parentColumns = ["id"], childColumns = ["pattern_row_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SymbolEntity::class, parentColumns = ["code"], childColumns = ["symbol_code"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = SymbolEntity::class, parentColumns = ["code"], childColumns = ["back_symbol_code"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["symbol_code"]), Index(value = ["back_symbol_code"]), Index(value = ["repeat_code"])]
)
data class PatternOperationEntity(
    @ColumnInfo(name = "pattern_row_id") val patternRowId: Long,
    @ColumnInfo(name = "column_start") val columnStart: Int,
    @ColumnInfo(name = "symbol_code") val symbolCode: String,
    @ColumnInfo(name = "back_symbol_code") val backSymbolCode: String? = null,
    @ColumnInfo(name = "color_hex") val colorHex: String? = null,
    @ColumnInfo(name = "column_span_override") val columnSpanOverride: Int? = null,
    @ColumnInfo(name = "row_span_override") val rowSpanOverride: Int? = null,
    @ColumnInfo(name = "repeat_code") val repeatCode: String? = null
)
