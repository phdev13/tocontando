package com.phdev.quantofalta.core.database

import androidx.room.ColumnInfo

data class WidgetEventData(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "targetDate") val targetDate: Long,
    @ColumnInfo(name = "colorArgb") val colorArgb: Int,
    @ColumnInfo(name = "isPrivate") val isPrivate: Boolean,
    @ColumnInfo(name = "isCompleted") val isCompleted: Boolean,
    @ColumnInfo(name = "coverImageUri") val coverImageUri: String?,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long
)
