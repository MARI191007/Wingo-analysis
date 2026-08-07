package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PeriodUtils {

    /**
     * Normalizes any raw period ID string into standard canonical period ID format
     * that matches Wingo live draw game issue numbers (e.g. "20260806100010850").
     */
    fun normalizePeriodId(
        rawId: String,
        gameMode: String,
        timestampMs: Long = System.currentTimeMillis()
    ): String {
        val clean = rawId.filter { it.isDigit() }
        if (clean.isBlank()) return rawId

        // If clean is already a full period ID (e.g. 10-20 digits like 20260806100010850), preserve it directly
        if (clean.length >= 10) {
            return clean
        }

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val datePrefix = sdf.format(Date(timestampMs))

        val typeCode = when (gameMode) {
            "3Min" -> "10002"
            "5Min" -> "10003"
            "10Min" -> "10004"
            "30s" -> "10005"
            else -> "10001"
        }

        val index4 = if (clean.length >= 4) clean.takeLast(4) else clean.padStart(4, '0')

        return "$datePrefix$typeCode$index4"
    }
}

