package com.aquiles.twinminddemo.api.response

import com.aquiles.twinminddemo.api.requests.GeminiUsageMetadata

data class GeminiSummaryResponse(
    val candidates: List<GeminiCandidate>,
    val usageMetadata: GeminiUsageMetadata? = null,
    val modelVersion: String? = null
)

fun GeminiSummaryResponse.extractText(): String =
    candidates.firstOrNull()
        ?.content
        ?.parts
        ?.firstOrNull()
        ?.text
        .orEmpty()
        .trim()
