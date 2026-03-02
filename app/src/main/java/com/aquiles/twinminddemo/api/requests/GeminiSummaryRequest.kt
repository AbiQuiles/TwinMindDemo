package com.aquiles.twinminddemo.api.requests

import com.aquiles.twinminddemo.api.GeminiContent
import com.aquiles.twinminddemo.api.GeminiGenerationConfig
import com.aquiles.twinminddemo.api.GeminiPart
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.generationConfig

data class GeminiSummaryRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
) {
    companion object {
        fun fromTranscript(transcript: String): GeminiSummaryRequest {
            return GeminiSummaryRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = """
                                    Analyze this meeting transcript and return a JSON object with exactly these fields:
                                    {
                                      "title": "concise meeting title",
                                      "summary": "2-3 paragraph summary",
                                      "actionItems": ["action 1", "action 2"],
                                      "keyPoints": ["point 1", "point 2"]
                                    }
                                    Transcript:
                                    $transcript
                                """.trimIndent()
                            )
                        )
                    )
                )
            )
        }
    }
}