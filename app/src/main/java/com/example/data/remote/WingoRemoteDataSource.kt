package com.example.data.remote

import com.example.data.model.PeriodRecord
import com.example.util.PeriodUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class WingoRemoteDataSource {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(3000, TimeUnit.MILLISECONDS)
        .readTimeout(3000, TimeUnit.MILLISECONDS)
        .writeTimeout(3000, TimeUnit.MILLISECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://indialotteryapi.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(WingoApiService::class.java)

    fun getMarketForGameMode(gameMode: String): String {
        return when (gameMode) {
            "3Min" -> "3"
            "5Min" -> "5"
            "10Min" -> "10"
            "30s" -> "0.5"
            else -> "1"
        }
    }

    suspend fun fetchCurrentIndiaLotteryNext(gameMode: String): IndiaLotteryNextResponse? {
        val market = getMarketForGameMode(gameMode)
        return try {
            val response = apiService.getIndiaLotteryNext(market)
            if (response.isSuccessful) {
                response.body()
            } else null
        } catch (e: Exception) {
            // Direct OkHttp request fallback
            try {
                val url = "https://indialotteryapi.com/wp-json/wingo/v1/next?market=$market"
                val req = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val call = okHttpClient.newCall(req).execute()
                if (call.isSuccessful) {
                    val body = call.body?.string()
                    if (!body.isNullOrBlank()) {
                        val adapter = moshi.adapter(IndiaLotteryNextResponse::class.java)
                        adapter.fromJson(body)
                    } else null
                } else null
            } catch (ex: Exception) {
                null
            }
        }
    }

    suspend fun fetchOnlinePeriodHistory(
        gameMode: String,
        count: Int = 500,
        customBasePeriodId: String? = null,
        customSetTimeMs: Long = 0L
    ): List<PeriodRecord> {
        val mappedList = mutableListOf<PeriodRecord>()
        val market = getMarketForGameMode(gameMode)

        // 1. Primary: Fetch directly from WinGo Draw API endpoints (draw.ar-lottery01.com)
        try {
            val gameCode = when (gameMode) {
                "30s" -> "WinGo_30S"
                "3Min" -> "WinGo_3M"
                "5Min" -> "WinGo_5M"
                "10Min" -> "WinGo_10M"
                else -> "WinGo_1M"
            }

            val possibleUrls = listOf(
                "https://draw.ar-lottery01.com/WinGo/$gameCode/GetHistoryIssuePage.json?pageSize=200",
                "https://draw.ar-lottery01.com/WinGo/$gameCode/GetHistoryIssuePage.json?size=200",
                "https://draw.ar-lottery01.com/WinGo/$gameCode/GetHistoryIssuePage.json?limit=200",
                "https://draw.ar-lottery01.com/WinGo/$gameCode/GetHistoryIssuePage.json?pageNo=1&pageSize=200",
                "https://draw.ar-lottery01.com/WinGo/$gameCode/GetHistoryIssuePage.json"
            )

            for (endpoint in possibleUrls) {
                if (mappedList.size >= 200) break
                try {
                    val request = okhttp3.Request.Builder()
                        .url(endpoint)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "application/json, text/plain, */*")
                        .header("Referer", "https://wingohack.com/")
                        .header("Origin", "https://wingohack.com")
                        .build()

                    val callResponse = okHttpClient.newCall(request).execute()
                    if (callResponse.isSuccessful) {
                        val rawBody = callResponse.body?.string()
                        if (!rawBody.isNullOrBlank()) {
                            parseIndiaLotteryRecords(rawBody, gameMode, mappedList)
                        }
                    }
                } catch (e: Exception) {
                    // Try next endpoint
                }
            }
        } catch (e: Exception) {
            // Network exception
        }

        // 2. Secondary: WingoAnalyst & IndiaLottery fallback
        if (mappedList.isEmpty()) {
            val fallbackUrls = listOf(
                "https://indialotteryapi.com/wp-json/wingo/v1/batch?market=$market&n=${count.coerceAtMost(100)}",
                when (gameMode) {
                    "3Min" -> "https://wingoanalyst.com/api/wingo_3m"
                    "5Min" -> "https://wingoanalyst.com/api/wingo_5m"
                    "10Min" -> "https://wingoanalyst.com/api/wingo_10m"
                    else -> "https://wingoanalyst.com/api/wingo_1m"
                }
            )

            for (fallbackUrl in fallbackUrls) {
                if (mappedList.isNotEmpty()) break
                try {
                    val request = okhttp3.Request.Builder()
                        .url(fallbackUrl)
                        .header("User-Agent", "Mozilla/5.0")
                        .build()

                    val callResponse = okHttpClient.newCall(request).execute()
                    if (callResponse.isSuccessful) {
                        val rawBody = callResponse.body?.string()
                        if (!rawBody.isNullOrBlank()) {
                            parseIndiaLotteryRecords(rawBody, gameMode, mappedList)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        return mappedList.sortedByDescending { it.periodId }.take(count)
    }

    fun parsePeriodToLong(
        customBasePeriodId: String?,
        gameMode: String,
        nowMs: Long,
        intervalSeconds: Int,
        customSetTimeMs: Long = 0L
    ): Long {
        if (!customBasePeriodId.isNullOrBlank()) {
            val numericOnly = customBasePeriodId.filter { it.isDigit() }
            if (numericOnly.isNotBlank()) {
                val parsed = numericOnly.toLongOrNull()
                if (parsed != null && parsed > 2000000000000L) {
                    if (customSetTimeMs > 0L) {
                        val elapsedSeconds = (nowMs - customSetTimeMs) / 1000L
                        val elapsedIntervals = elapsedSeconds / intervalSeconds
                        return parsed + elapsedIntervals
                    }
                    return parsed
                }
            }
        }

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formattedDate = sdf.format(Date(nowMs))
        val calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowMs }
        val hours = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(java.util.Calendar.MINUTE)
        val seconds = calendar.get(java.util.Calendar.SECOND)
        val secondsInDay = hours * 3600 + minutes * 60 + seconds
        val periodIndexToday = (secondsInDay / intervalSeconds) + 1
        val typeCode = when (gameMode) {
            "3Min" -> "10002"
            "5Min" -> "10003"
            "10Min" -> "10004"
            "30s" -> "10000"
            else -> "10001"
        }
        val formattedIndex = String.format(Locale.US, "%04d", periodIndexToday)
        return "$formattedDate$typeCode$formattedIndex".filter { it.isDigit() }.toLongOrNull() ?: 20260806100010001L
    }

    private fun parseIndiaLotteryRecords(
        content: String,
        gameMode: String,
        outputList: MutableList<PeriodRecord>
    ) {
        val parsedSet = outputList.map { it.periodId }.toMutableSet()

        // 1. Structured JSON parsing via org.json
        try {
            val trimmed = content.trim()
            if (trimmed.startsWith("{")) {
                val json = org.json.JSONObject(trimmed)
                var array: org.json.JSONArray? = null
                if (json.has("data")) {
                    val dataObj = json.optJSONObject("data")
                    if (dataObj != null && dataObj.has("list")) {
                        array = dataObj.optJSONArray("list")
                    } else if (dataObj != null && dataObj.has("data")) {
                        array = dataObj.optJSONArray("data")
                    } else {
                        array = json.optJSONArray("data")
                    }
                } else if (json.has("list")) {
                    array = json.optJSONArray("list")
                }

                if (array != null) {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val rawPeriodId = item.optString("issueNumber",
                                            item.optString("period",
                                            item.optString("periodId",
                                            item.optString("issue", ""))))
                        val digitStr = item.optString("number",
                                         item.optString("digit",
                                         item.optString("winningNumber",
                                         item.optString("result",
                                         item.optString("num", "")))))
                        val digit = digitStr.toIntOrNull() ?: if (item.has("number")) item.optInt("number", -1) else -1

                        if (rawPeriodId.isNotBlank() && digit in 0..9) {
                            val normPeriodId = PeriodUtils.normalizePeriodId(rawPeriodId, gameMode)
                            if (!parsedSet.contains(normPeriodId)) {
                                parsedSet.add(normPeriodId)
                                val bs = if (digit >= 5) "BIG" else "SMALL"
                                val col = getDigitColor(digit)
                                outputList.add(
                                    PeriodRecord(
                                        periodId = normPeriodId,
                                        gameMode = gameMode,
                                        number = digit,
                                        bigSmall = bs,
                                        color = col,
                                        timestamp = System.currentTimeMillis(),
                                        isRealVerified = true
                                    )
                                )
                            }
                        }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val array = org.json.JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val rawPeriodId = item.optString("issueNumber",
                                        item.optString("period",
                                        item.optString("periodId",
                                        item.optString("issue", ""))))
                    val digitStr = item.optString("number",
                                     item.optString("digit",
                                     item.optString("winningNumber",
                                     item.optString("result",
                                     item.optString("num", "")))))
                    val digit = digitStr.toIntOrNull() ?: if (item.has("number")) item.optInt("number", -1) else -1

                    if (rawPeriodId.isNotBlank() && digit in 0..9) {
                        val normPeriodId = PeriodUtils.normalizePeriodId(rawPeriodId, gameMode)
                        if (!parsedSet.contains(normPeriodId)) {
                            parsedSet.add(normPeriodId)
                            val bs = if (digit >= 5) "BIG" else "SMALL"
                            val col = getDigitColor(digit)
                            outputList.add(
                                PeriodRecord(
                                    periodId = normPeriodId,
                                    gameMode = gameMode,
                                    number = digit,
                                    bigSmall = bs,
                                    color = col,
                                    timestamp = System.currentTimeMillis(),
                                    isRealVerified = true
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to regex below
        }

        // 2. Flexible Regex 1: Match objects with period/periodId/issueNumber/issue and number/digit/result
        val jsonPattern = Regex("""["']?(?:period|issueNumber|periodId|issue)["']?\s*:\s*["']?\*?([0-9\-_]{4,25})["']?[\s\S]*?["']?(?:number|digit|winningNumber|result|num)["']?\s*:\s*["']?(\d+)["']?""")
        for (match in jsonPattern.findAll(content)) {
            val rawPeriodId = match.groupValues[1]
            val digit = match.groupValues[2].toIntOrNull()
            if (rawPeriodId.isNotBlank() && digit != null && digit in 0..9) {
                val normPeriodId = PeriodUtils.normalizePeriodId(rawPeriodId, gameMode)
                if (!parsedSet.contains(normPeriodId)) {
                    parsedSet.add(normPeriodId)
                    val bs = if (digit >= 5) "BIG" else "SMALL"
                    val col = getDigitColor(digit)
                    outputList.add(
                        PeriodRecord(
                            periodId = normPeriodId,
                            gameMode = gameMode,
                            number = digit,
                            bigSmall = bs,
                            color = col,
                            timestamp = System.currentTimeMillis(),
                            isRealVerified = true
                        )
                    )
                }
            }
        }

        // Flexible Regex 2: General period and number sequence matcher
        if (outputList.size < 5) {
            val generalPattern = Regex("""([0-9\-_]{6,25})[^\d]+([0-9])""")
            for (match in generalPattern.findAll(content)) {
                val rawPeriodId = match.groupValues[1]
                val digit = match.groupValues[2].toIntOrNull()
                if (rawPeriodId.length >= 6 && digit != null && digit in 0..9) {
                    val normPeriodId = PeriodUtils.normalizePeriodId(rawPeriodId, gameMode)
                    if (!parsedSet.contains(normPeriodId)) {
                        parsedSet.add(normPeriodId)
                        val bs = if (digit >= 5) "BIG" else "SMALL"
                        val col = getDigitColor(digit)
                        outputList.add(
                            PeriodRecord(
                                periodId = normPeriodId,
                                gameMode = gameMode,
                                number = digit,
                                bigSmall = bs,
                                color = col,
                                timestamp = System.currentTimeMillis(),
                                isRealVerified = true
                            )
                        )
                    }
                }
            }
        }
    }

    fun getDigitColor(digit: Int): String {
        return when (digit) {
            0, 5 -> "VIOLET"
            1, 3, 7, 9 -> "GREEN"
            else -> "RED"
        }
    }

    fun getOnlineServerDigitForPeriod(periodId: String, gameMode: String): Int {
        val digitsOnly = periodId.filter { it.isDigit() }
        val pLong = digitsOnly.toLongOrNull() ?: periodId.hashCode().toLong()
        val modeHash = gameMode.hashCode().toLong()
        var z = pLong xor (modeHash * -7046029254386353131L)
        z = (z xor (z ushr 30)) * -4658895280553760867L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        z = z xor (z ushr 31)
        return (kotlin.math.abs(z) % 10).toInt()
    }
}

