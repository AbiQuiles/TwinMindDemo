package com.aquiles.twinminddemo.api

import com.google.gson.annotations.SerializedName

data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String = "user"
)

data class GeminiPart(
    val text: String? = null,
    @SerializedName("inline_data")
    val inlineData: InlineData? = null
)

data class InlineData(
    @SerializedName("mime_type")
    val mimeType: String,
    val data: String
)

data class GeminiGenerationConfig(
    @SerializedName("response_mime_type")
    val responseMimeType: String = "application/json"
)

data class SummaryContent(
    val title: String = "",
    val summary: String = "",
    val actionItems: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList()
)