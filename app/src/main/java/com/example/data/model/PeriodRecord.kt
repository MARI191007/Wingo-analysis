package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_records")
data class PeriodRecord(
    @PrimaryKey val periodId: String,
    val gameMode: String, // "1Min", "3Min", "5Min", "10Min"
    val number: Int, // 0..9
    val bigSmall: String, // "BIG" (5-9) or "SMALL" (0-4)
    val color: String, // "GREEN", "RED", "VIOLET"
    val timestamp: Long = System.currentTimeMillis()
)
