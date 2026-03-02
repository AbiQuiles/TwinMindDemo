package com.aquiles.twinminddemo.ui.components.record

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.twinminddemo.data.models.SessionStateHolder
import com.aquiles.twinminddemo.data.repositories.RecordingRepository
import com.aquiles.twinminddemo.services.RecordingService
import com.aquiles.twinminddemo.services.RecordingStatus
import com.aquiles.twinminddemo.ui.components.summary.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val sessionStateHolder: SessionStateHolder
) : ViewModel() {

    private val _status = MutableStateFlow<RecordingStatus>(RecordingStatus.Idle)
    val status: StateFlow<RecordingStatus> = _status

    private val _timerText = MutableStateFlow("00:00")
    val timerText: StateFlow<String> = _timerText

    private val _permissionState = MutableStateFlow(PermissionState.check(context))
    val permissionState: StateFlow<PermissionState> = _permissionState

    private var currentMeetingId: String? = null
    private var timerJob: Job? = null

    fun onPermissionResult(permission: String, granted: Boolean) {
        _permissionState.update { current ->
            current.copy(
                phoneStateGranted = if (permission == Manifest.permission.READ_PHONE_STATE)
                    granted else current.phoneStateGranted,
                recordAudioGranted = if (permission == Manifest.permission.RECORD_AUDIO)
                    granted else current.recordAudioGranted
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun startRecording() {
        if (!_permissionState.value.recordAudioGranted) {
            _status.value = RecordingStatus.Error("Microphone permission required")
            return
        }

        val sessionId = UUID.randomUUID().toString()
        sessionStateHolder.setSessionId(sessionId)

        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_SESSION_ID, sessionId)
        }
        context.startForegroundService(intent)
        _status.value = RecordingStatus.Recording
        startTimer()
    }

    fun stopRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        context.startService(intent)
        timerJob?.cancel()
        _status.value = RecordingStatus.Stopped
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            var seconds = 0L
            while (isActive && _status.value == RecordingStatus.Recording) {
                delay(1000)
                seconds++
                _timerText.value = "%02d:%02d".format(seconds / 60, seconds % 60)
            }
        }
    }
}