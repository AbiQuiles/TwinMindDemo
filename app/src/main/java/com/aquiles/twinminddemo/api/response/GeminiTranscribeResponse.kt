package com.aquiles.twinminddemo.api.response

import com.aquiles.twinminddemo.api.GeminiContent
import com.aquiles.twinminddemo.api.requests.GeminiUsageMetadata

data class GeminiTranscribeResponse(
    val candidates: List<GeminiCandidate>,
    val usageMetadata: GeminiUsageMetadata? = null,
    val modelVersion: String? = null
)

data class GeminiCandidate(val content: GeminiContent)

fun GeminiTranscribeResponse.extractText(): String =
    candidates.firstOrNull()
        ?.content
        ?.parts
        ?.firstOrNull()
        ?.text
        .orEmpty()
        .trim()
