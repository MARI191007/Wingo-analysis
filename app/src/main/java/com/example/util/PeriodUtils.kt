package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PeriodUtils {

    /**
     * Normalizes any raw period ID string into standard canonical 14-digit format
     * matching Wingo live draw game issue numbers (e.g. "20260808010045").
     */
    fun normalizePeriodId(
        rawId: String,
        gameMode: String,
        timestampMs: Long = System.currentTimeMillis()
    ): String {
        val clean = rawId.filter { it.isDigit() }
        if (clean.isBlank()) return rawId

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        // Extract 8-digit date prefix if present (e.g. starting with 202)
        val datePrefix = if (clean.length >= 8 && clean.startsWith("202")) {
            clean.substring(0, 8)
        } else {
            sdf.format(Date(timestampMs))
        }

        val modeCode = when (gameMode) {
            "3Min" -> "03"
            "5Min" -> "05"
            "10Min" -> "10"
            "30s" -> "30"
            else -> "01"
        }

        val index4 = if (clean.length >= 4) clean.takeLast(4) else clean.padStart(4, '0')

        return "$datePrefix$modeCode$index4"
    }
}


