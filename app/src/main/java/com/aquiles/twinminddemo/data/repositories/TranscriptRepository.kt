package com.aquiles.twinminddemo.data.repositories

import android.util.Log
import com.aquiles.twinminddemo.api.GeminiApi
import com.aquiles.twinminddemo.api.requests.GeminiTranscribeRequest
import com.aquiles.twinminddemo.api.response.extractText
import com.aquiles.twinminddemo.data.dao.AudioChunkDao
import com.aquiles.twinminddemo.data.dao.TranscriptDao
import com.aquiles.twinminddemo.data.entities.AudioChunkEntity
import com.aquiles.twinminddemo.data.entities.TranscriptEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val chunkDao: AudioChunkDao,
    private val geminiApi: GeminiApi
) {
    suspend fun transcribeChunk(chunk: AudioChunkEntity, sessionId: String) {
        chunkDao.updateStatus(chunk.id, "IN_PROGRESS")
        try {
            val file = File(chunk.filePath)
            if (!file.exists()) {
                chunkDao.updateStatus(chunk.id, "FAILED")
                return
            }
            val request = GeminiTranscribeRequest.fromAudioFile(file)
            val response = geminiApi.transcribeAudio(request = request)

            transcriptDao.insert(
                TranscriptEntity(
                    sessionId = sessionId,
                    chunkId = chunk.id,
                    chunkIndex = chunk.chunkIndex,
                    text = response.extractText()
                )
            )
            chunkDao.updateStatus(chunk.id, "DONE")
        } catch (e: Exception) {
            Log.e("TranscriptRepository", "Failed to transcribe chunk ${chunk.id}", e)
            chunkDao.updateStatus(chunk.id, "FAILED")
            throw e
        }
    }

    suspend fun getPendingChunks(sessionId: String): List<AudioChunkEntity> =
        chunkDao.getPendingChunks(sessionId)
}