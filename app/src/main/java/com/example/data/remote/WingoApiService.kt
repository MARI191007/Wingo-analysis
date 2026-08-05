package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class WingoApiResponse(
    @Json(name = "code") val code: Int? = 200,
    @Json(name = "msg") val message: String? = null,
    @Json(name = "data") val data: WingoDataWrapper? = null,
    @Json(name = "list") val rawList: List<WingoRawRecord>? = null
)

@JsonClass(generateAdapter = true)
data class WingoDataWrapper(
    @Json(name = "list") val list: List<WingoRawRecord>? = null,
    @Json(name = "pageNo") val pageNo: Int? = 1,
    @Json(name = "total") val total: Int? = 0
)

@JsonClass(generateAdapter = true)
data class WingoRawRecord(
    @Json(name = "period") val period: String? = null,
    @Json(name = "issueNumber") val issueNumber: String? = null,
    @Json(name = "periodId") val periodId: String? = null,
    @Json(name = "number") val number: Int? = null,
    @Json(name = "winningNumber") val winningNumber: Int? = null,
    @Json(name = "result") val result: String? = null,
    @Json(name = "bigSmall") val bigSmall: String? = null,
    @Json(name = "color") val color: String? = null,
    @Json(name = "time") val time: String? = null,
    @Json(name = "timestamp") val timestamp: Long? = null
)

interface WingoApiService {

    @GET
    suspend fun fetchDirectUrl(
        @Url url: String
    ): Response<WingoApiResponse>

    @GET("api/webapi/GetNoHeaderList")
    suspend fun getNoHeaderList(
        @Query("typeId") typeId: Int,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 100
    ): Response<WingoApiResponse>

    @GET("api/wingo/history")
    suspend fun getYaarwinHistory(
        @Query("mode") mode: String,
        @Query("limit") limit: Int = 500
    ): Response<WingoApiResponse>
}
