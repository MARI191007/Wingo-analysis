package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class IndiaLotteryNextResponse(
    @Json(name = "period") val period: String? = null,
    @Json(name = "remain") val remain: Int? = null,
    @Json(name = "seconds_per_period") val secondsPerPeriod: Int? = null
)

@JsonClass(generateAdapter = true)
data class WingoRawRecord(
    @Json(name = "period") val period: String? = null,
    @Json(name = "periodId") val periodId: String? = null,
    @Json(name = "issueNumber") val issueNumber: String? = null,
    @Json(name = "number") val number: Int? = null,
    @Json(name = "digit") val digit: Int? = null,
    @Json(name = "winningNumber") val winningNumber: Int? = null,
    @Json(name = "result") val result: String? = null,
    @Json(name = "big_small") val bigSmallUnderscore: String? = null,
    @Json(name = "bigSmall") val bigSmallCamel: String? = null,
    @Json(name = "color") val color: String? = null,
    @Json(name = "time") val time: String? = null,
    @Json(name = "timestamp") val timestamp: Long? = null
)

interface WingoApiService {

    @GET("wp-json/wingo/v1/next")
    suspend fun getIndiaLotteryNext(
        @Query("market") market: String
    ): Response<IndiaLotteryNextResponse>

    @GET("wp-json/wingo/v1/batch")
    suspend fun getIndiaLotteryBatch(
        @Query("market") market: String,
        @Query("n") n: Int = 40
    ): Response<ResponseBody>

    @GET
    suspend fun fetchDirectUrl(
        @Url url: String
    ): Response<ResponseBody>
}

