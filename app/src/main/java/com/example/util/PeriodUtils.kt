package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PeriodUtils {

    /**
     * Normalizes any raw period ID string (e.g. "*010570", "010570", "10570", "20260805100010570", "2026080510570")
     * into the standard 13-digit canonical period ID format (e.g. "2026080510570")
     * that matches Wingo server clocks and local database primary keys.
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

        val datePrefix = if (clean.length >= 12 && clean.startsWith("202")) {
            clean.substring(0, 8)
        } else {
            sdf.format(Date(timestampMs))
        }

        val index4 = if (clean.length >= 4) clean.takeLast(4) else clean.padStart(4, '0')
        val modePrefix = when (gameMode) {
            "3Min" -> "3"
            "5Min" -> "5"
            "10Min" -> "10"
            else -> "1"
        }

        return "$datePrefix$modePrefix$index4"
    }
}
