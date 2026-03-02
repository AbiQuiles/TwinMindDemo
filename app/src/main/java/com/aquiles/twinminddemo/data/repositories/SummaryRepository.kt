package com.aquiles.twinminddemo.data.repositories

import com.aquiles.twinminddemo.api.GeminiApi
import com.aquiles.twinminddemo.api.SummaryContent
import com.aquiles.twinminddemo.api.requests.GeminiSummaryRequest
import com.aquiles.twinminddemo.api.response.extractText
import com.aquiles.twinminddemo.data.dao.SummaryDao
import com.aquiles.twinminddemo.data.dao.TranscriptDao
import com.aquiles.twinminddemo.data.entities.SummaryEntity
import com.aquiles.twinminddemo.data.models.Summary
import com.aquiles.twinminddemo.data.models.SummaryStatus
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SummaryRepository @Inject constructor(
    private val summaryDao: SummaryDao,
    private val transcriptDao: TranscriptDao,
    private val geminiApi: GeminiApi
) {
    fun getSummaryForMeeting(sessionId: String): Flow<Summary?> =
        summaryDao.getSummaryForSession(sessionId).map { entity ->
            entity?.toDomain()
        }

    suspend fun createSummary(sessionId: String) =
        summaryDao.upsert(SummaryEntity(sessionId = sessionId, status = "GENERATING"))

    suspend fun generateSummary(sessionId: String) {
        val texts = transcriptDao.getOrderedTexts(sessionId)
        val fullTranscript = texts.joinToString("\n")
        val request = GeminiSummaryRequest.fromTranscript(fullTranscript)
        val response = geminiApi.generateSummary(request = request)
        val parsed = Gson().fromJson(response.extractText(), SummaryContent::class.java)

        summaryDao.updateContent(
            sessionId = sessionId,
            title = parsed.title,
            summary = parsed.summary,
            actionItems = Gson().toJson(parsed.actionItems),
            keyPoints = Gson().toJson(parsed.keyPoints)
        )
    }
}

private fun SummaryEntity.toDomain(): Summary = Summary(
    sessionId = sessionId,
    title = title,
    summary = summary,
    actionItems = runCatching {
        Gson().fromJson(actionItems, Array<String>::class.java).toList()
    }.getOrDefault(emptyList()),
    keyPoints = runCatching {
        Gson().fromJson(keyPoints, Array<String>::class.java).toList()
    }.getOrDefault(emptyList()),
    status = when (status) {
        "GENERATING" -> SummaryStatus.GENERATING
        "DONE" -> SummaryStatus.DONE
        "FAILED" -> SummaryStatus.FAILED
        else -> SummaryStatus.IDLE
    },
    errorMessage = errorMessage
)