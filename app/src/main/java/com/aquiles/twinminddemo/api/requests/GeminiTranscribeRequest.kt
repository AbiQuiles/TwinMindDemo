package com.aquiles.twinminddemo.api.requests

import android.util.Base64
import com.aquiles.twinminddemo.api.GeminiContent
import com.aquiles.twinminddemo.api.GeminiPart
import com.aquiles.twinminddemo.api.InlineData
import java.io.File

data class GeminiTranscribeRequest(
    val contents: List<GeminiContent>
) {
    companion object {
        fun fromAudioFile(file: File): GeminiTranscribeRequest {
            val base64Audio = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            val mimeType = "audio/mp4"
            return GeminiTranscribeRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = "Transcribe this audio accurately. Return only the transcript text, no commentary."),
                            GeminiPart(inlineData = InlineData(
                                mimeType = mimeType,
                                data = base64Audio
                            )
                            )
                        )
                    )
                )
            )
        }
    }
}