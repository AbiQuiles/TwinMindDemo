package com.aquiles.twinminddemo.ui.components.summary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aquiles.twinminddemo.data.entities.SummaryEntity
import com.aquiles.twinminddemo.data.models.SessionStateHolder
import com.aquiles.twinminddemo.data.models.Summary
import com.aquiles.twinminddemo.data.repositories.SummaryRepository
import com.aquiles.twinminddemo.services.SummaryWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val summaryRepository: SummaryRepository,
    private val sessionStateHolder: SessionStateHolder,
) : ViewModel() {

    private val _summary = MutableStateFlow<Summary?>(null)
    val summary: StateFlow<Summary?> = _summary

    private var currentMeetingId: String? = null

    init {
        viewModelScope.launch {
            sessionStateHolder.currentSessionId.collect { sessionId ->
                if (sessionId != null) observeSummary(sessionId)
                else _summary.value = null
            }
        }
    }

    private fun observeSummary(sessionId: String) {
        viewModelScope.launch {
            summaryRepository.getSummaryForMeeting(sessionId).collect {
                _summary.value = it
            }
        }
    }

    fun generateSummary() {
        val sessionId = sessionStateHolder.currentSessionId.value ?: return
        viewModelScope.launch {
            summaryRepository.createSummary(sessionId)
            val request = OneTimeWorkRequestBuilder<SummaryWorker>()
                .setInputData(workDataOf("sessionId" to sessionId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "summary_$sessionId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    fun loadSummary(meetingId: String) {
        currentMeetingId = meetingId
        viewModelScope.launch {
            summaryRepository.getSummaryForMeeting(meetingId).collect {
                _summary.value = it
            }
        }
    }

    fun generateSummary(meetingId: String) {
        currentMeetingId = meetingId
        viewModelScope.launch {
            summaryRepository.createSummary(meetingId)
            val request = OneTimeWorkRequestBuilder<SummaryWorker>()
                .setInputData(workDataOf("meetingId" to meetingId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "summary_$meetingId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    fun retry() {
        currentMeetingId?.let { generateSummary(it) }
    }
}