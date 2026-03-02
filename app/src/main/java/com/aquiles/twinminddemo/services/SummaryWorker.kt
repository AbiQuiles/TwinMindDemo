package com.aquiles.twinminddemo.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aquiles.twinminddemo.api.GeminiApi
import com.aquiles.twinminddemo.api.SummaryContent
import com.aquiles.twinminddemo.api.requests.GeminiSummaryRequest
import com.aquiles.twinminddemo.api.response.extractText
import com.aquiles.twinminddemo.data.dao.SummaryDao
import com.aquiles.twinminddemo.data.dao.TranscriptDao
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transcriptDao: TranscriptDao,
    private val summaryDao: SummaryDao,
    private val geminiApi: GeminiApi
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString("sessionId") ?: return Result.failure()

        return try {
            val texts = transcriptDao.getOrderedTexts(sessionId)
            val fullTranscript = texts.joinToString("\n")
            val request = GeminiSummaryRequest.fromTranscript(fullTranscript)
            val response = geminiApi.generateSummary(request = request)

            // JSON to text parse
            val parsed = Gson().fromJson(
                response.extractText(),
                SummaryContent::class.java
            )

            summaryDao.updateContent(
                sessionId = sessionId,
                title = parsed.title,
                summary = parsed.summary,
                actionItems = Gson().toJson(parsed.actionItems),
                keyPoints = Gson().toJson(parsed.keyPoints)
            )
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}