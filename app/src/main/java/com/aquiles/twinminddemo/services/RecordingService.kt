package com.aquiles.twinminddemo.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller.EXTRA_SESSION_ID
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.aquiles.twinminddemo.util.AudioFocusManager
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aquiles.twinminddemo.R
import com.aquiles.twinminddemo.data.dao.AudioChunkDao
import com.aquiles.twinminddemo.data.entities.AudioChunkEntity
import com.aquiles.twinminddemo.data.repositories.RecordingRepository
import com.aquiles.twinminddemo.util.SilenceDetector
import com.aquiles.twinminddemo.util.StorageChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class RecordingStatus {
    object Idle : RecordingStatus()
    object Recording : RecordingStatus()
    data class Paused(val reason: String) : RecordingStatus()
    object Stopped : RecordingStatus()
    data class Error(val message: String) : RecordingStatus()
}

@AndroidEntryPoint
class RecordingService : LifecycleService() {

    @Inject
    lateinit var recordingRepository: RecordingRepository

    private var mediaRecorder: MediaRecorder? = null
    private var currentMeetingId: String? = null
    private var chunkIndex = 0
    private var currentSessionId: String? = null
    private var chunkStartTime = 0L
    private val CHUNK_DURATION_MS = 30_000L
    private val OVERLAP_MS = 2_000L

    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var silenceJob: Job? = null

    private lateinit var audioFocusManager: AudioFocusManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var phoneStateListener: PhoneStateListener
    private lateinit var headsetReceiver: BroadcastReceiver
    private lateinit var storageChecker: StorageChecker
    private lateinit var silenceDetector: SilenceDetector

    private val _status = MutableStateFlow<RecordingStatus>(RecordingStatus.Idle)
    val status: StateFlow<RecordingStatus> = _status

    private var totalSeconds = 0L

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "recording_channel"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        audioFocusManager = AudioFocusManager(
            this,
            ::onAudioFocusLost,
            ::onAudioFocusGained
        )

        storageChecker = StorageChecker(this)
        silenceDetector = SilenceDetector()
        registerHeadsetReceiver()
        registerPhoneStateListener()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                    ?: UUID.randomUUID().toString()
                startRecording(sessionId)
            }
            ACTION_STOP -> stopRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startRecording(sessionId: String) {
        if (!storageChecker.hasEnoughStorage()) {
            showErrorNotification("Recording stopped - Low storage")
            stopSelf()
            return
        }

        currentSessionId = sessionId
        chunkIndex = 0

        registerPhoneStateListener()
        audioFocusManager.requestFocus()
        _status.value = RecordingStatus.Recording
        startForeground(NOTIFICATION_ID, buildNotification("Recording..."))
        startChunkRecording()
        startTimer()
        startSilenceDetection()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startChunkRecording() {
        recordingJob = lifecycleScope.launch {
            while (isActive && _status.value == RecordingStatus.Recording) {
                if (!storageChecker.hasEnoughStorage()) {
                    _status.value = RecordingStatus.Error("Low storage")
                    showErrorNotification("Recording stopped - Low storage")
                    stopRecording()
                    break
                }

                val overlapStart = if (chunkIndex > 0) OVERLAP_MS else 0L
                val chunkFile = recordChunk(overlapStart)

                chunkFile?.let { file ->
                    val chunk = AudioChunkEntity(
                        sessionId = currentSessionId!!,
                        chunkIndex = chunkIndex++,
                        filePath = file.absolutePath,
                        durationMs = CHUNK_DURATION_MS
                    )
                    recordingRepository.saveChunk(chunk)
                    enqueueTranscriptionWorker(chunk.id, currentSessionId!!)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun recordChunk(leadMs: Long): File? = withContext(Dispatchers.IO) {
        val file = File(
            getChunkDir(),
            "chunk_${chunkIndex}_${System.currentTimeMillis()}.m4a"
        )

        try {
            val recorder = MediaRecorder(this@RecordingService).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder

            chunkStartTime = System.currentTimeMillis() - leadMs
            delay(CHUNK_DURATION_MS)

            recorder.stop()
            recorder.release()
            mediaRecorder = null
            file  // ← returns File, which has absolutePath
        } catch (e: Exception) {
            Log.e("RecordingService", "Chunk recording failed", e)
            null
        }
    }

    private fun getChunkDir(): File {
        val dir = File(filesDir, "chunks/${currentSessionId}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun startTimer() {
        timerJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                if (_status.value == RecordingStatus.Recording) {
                    totalSeconds++
                    updateNotification(
                        buildNotification(
                            formatDuration(totalSeconds)
                        )
                    )
                }
            }
        }
    }

    private fun startSilenceDetection() {
        silenceJob = lifecycleScope.launch {
            delay(10_000)
            if (silenceDetector.isSilent(mediaRecorder)) {
                updateNotification(buildNotification("No audio detected - Check microphone"))
            }
        }
    }

    private fun pauseRecording(reason: String) {
        if (_status.value !is RecordingStatus.Recording) return
        mediaRecorder?.pause()
        _status.value = RecordingStatus.Paused(reason)
        updateNotification(buildPausedNotification(reason))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun resumeRecording() {
        if (_status.value !is RecordingStatus.Paused) return
        audioFocusManager.requestFocus()
        mediaRecorder?.resume()
        _status.value = RecordingStatus.Recording
        updateNotification(buildNotification("Recording..."))
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        timerJob?.cancel()
        silenceJob?.cancel()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null

        lifecycleScope.launch {
            currentMeetingId?.let {
                enqueueTerminationWorker(it)
            }
        }
        _status.value = RecordingStatus.Stopped
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // Audio Focus
    private fun onAudioFocusLost() = pauseRecording("Paused – Audio focus lost")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun onAudioFocusGained() = resumeRecording()

    // Phone State
    private fun registerPhoneStateListener() {
        // Guard added — service won't crash if permission not granted yet
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w("RecordingService", "READ_PHONE_STATE not granted — skipping call detection")
            return
        }

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            @RequiresApi(Build.VERSION_CODES.S)
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING,
                    TelephonyManager.CALL_STATE_OFFHOOK -> pauseRecording("Paused - Phone call")
                    TelephonyManager.CALL_STATE_IDLE ->
                        if (_status.value is RecordingStatus.Paused) resumeRecording()
                }
            }
        }
        @Suppress("DEPRECATION")
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    // Headset
    private fun registerHeadsetReceiver() {
        headsetReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val connected = intent.getIntExtra("state", 0) == 1
                val type = if (intent.action == AudioManager.ACTION_HEADSET_PLUG) "Wired" else "Bluetooth"
                val msg = if (connected) "$type headset connected" else "$type headset disconnected"
                showTransientNotification(msg)
                // Audio source switches automatically; recording continues
            }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        registerReceiver(headsetReceiver, filter)
    }

    // Notifications
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recording",
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(statusText: String): Notification {
        val stopIntent = PendingIntent.getService(this, 0,
            Intent(this, RecordingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meeting Recorder")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.mic_24px)
            .addAction(R.drawable.stop_24px, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun buildPausedNotification(reason: String): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, RecordingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RecordingService::class.java).apply { action = ACTION_RESUME },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meeting Recorder")
            .setContentText(reason)
            .setSmallIcon(R.drawable.pause_24px)
            .addAction(R.drawable.play_arrow_24px, "Resume", resumeIntent)
            .addAction(R.drawable.stop_24px, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun showErrorNotification(msg: String) {
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Error")
            .setContentText(msg)
            .setSmallIcon(R.drawable.error_24px)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID + 1, n)
    }

    private fun showTransientNotification(msg: String) {
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Microphone Source Changed").setContentText(msg)
            .setSmallIcon(R.drawable.mic_24px)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID + 2, n)
    }

    private fun updateNotification(notification: Notification) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun formatDuration(s: Long) = "%02d:%02d".format(s / 60, s % 60)

    private fun enqueueTranscriptionWorker(chunkId: String, meetingId: String) {
        val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workDataOf("chunkId" to chunkId, "meetingId" to meetingId))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "transcribe_$chunkId", ExistingWorkPolicy.KEEP, request
        )
    }

    private fun enqueueTerminationWorker(meetingId: String) {
        val request = OneTimeWorkRequestBuilder<TerminationWorker>()
            .setInputData(
                workDataOf("meetingId" to meetingId)
            )
            .build()
        WorkManager.getInstance(this).enqueue(request)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(headsetReceiver)
        @Suppress("DEPRECATION")
        telephonyManager.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_NONE
        )
        audioFocusManager.abandonFocus()
    }
}
