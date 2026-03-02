package com.aquiles.twinminddemo.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aquiles.twinminddemo.data.repositories.TranscriptRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptRepository: TranscriptRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString("sessionId") ?: return Result.failure()
        val chunks = transcriptRepository.getPendingChunks(sessionId)
        var anyFailed = false

        for (chunk in chunks) {
            try {
                transcriptRepository.transcribeChunk(chunk, sessionId)
            } catch (e: Exception) {
                anyFailed = true
            }
        }
        return if (anyFailed) Result.retry() else Result.success()
    }
}