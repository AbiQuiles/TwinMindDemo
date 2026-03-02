package com.aquiles.twinminddemo.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aquiles.twinminddemo.data.repositories.SummaryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TerminationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val summaryRepository: SummaryRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString("sessionId") ?: return Result.failure()

        return try {
            summaryRepository.generateSummary(sessionId)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}