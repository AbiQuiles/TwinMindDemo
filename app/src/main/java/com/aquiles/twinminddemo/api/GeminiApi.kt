package com.aquiles.twinminddemo.api

import com.aquiles.twinminddemo.api.requests.GeminiSummaryRequest
import com.aquiles.twinminddemo.api.response.GeminiSummaryResponse
import com.aquiles.twinminddemo.api.requests.GeminiTranscribeRequest
import com.aquiles.twinminddemo.api.response.GeminiTranscribeResponse
import com.aquiles.twinminddemo.BuildConfig
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface GeminiApi {

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun transcribeAudio(
        @Query("key") apiKey: String = BuildConfig.GEMINI_API_KEY,
        @Body request: GeminiTranscribeRequest
    ): GeminiTranscribeResponse

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateSummary(
        @Query("key") apiKey: String = BuildConfig.GEMINI_API_KEY,
        @Body request: GeminiSummaryRequest
    ): GeminiSummaryResponse
}