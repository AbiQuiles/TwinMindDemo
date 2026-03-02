package com.aquiles.twinminddemo.ui.components.record

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquiles.twinminddemo.services.RecordingStatus

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun RecordingView(
    modifier: Modifier = Modifier,
    viewModel: RecordingViewModel = hiltViewModel(),
    onStopRecording: () -> Unit = {}
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val timer by viewModel.timerText.collectAsStateWithLifecycle()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()

    // Permission launchers
    val recordAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(Manifest.permission.RECORD_AUDIO, granted)
    }

    val phoneStateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(Manifest.permission.READ_PHONE_STATE, granted)
    }

    LaunchedEffect(Unit) {
        if (!permissionState.recordAudioGranted) {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        if (!permissionState.phoneStateGranted) {
            phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timer,
            style = MaterialTheme.typography.displayMedium,
        )

        Spacer(Modifier.height(8.dp))

        StatusBadgeView(status)

        if (!permissionState.canDetectCalls) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "⚠ Call detection disabled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(Modifier.height(16.dp))

        RecordButtonView(
            status = status,
            enabled = permissionState.canRecord,
            onStart = { viewModel.startRecording() },
            onStop = {
                viewModel.stopRecording()
                onStopRecording()
            }
        )

        // Show message if mic permission denied
        if (!permissionState.canRecord) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Microphone permission required to record",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun StatusBadgeView(status: RecordingStatus) {
    val (text, color) = when (status) {
        is RecordingStatus.Recording -> "Recording..." to MaterialTheme.colorScheme.error
        is RecordingStatus.Paused -> status.reason to MaterialTheme.colorScheme.tertiary
        is RecordingStatus.Stopped -> "Stopped" to MaterialTheme.colorScheme.outline
        is RecordingStatus.Error -> status.message to MaterialTheme.colorScheme.error
        else -> "Ready" to MaterialTheme.colorScheme.outline
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (status is RecordingStatus.Recording) {
            PulsingDotView(color = color)
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
fun PulsingDotView(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
fun RecordButtonView(
    status: RecordingStatus,
    enabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isRecording = status is RecordingStatus.Recording || status is RecordingStatus.Paused
    FloatingActionButton(
        onClick = { if (isRecording) onStop() else onStart() },
        containerColor = when {
            !enabled -> MaterialTheme.colorScheme.outline
            isRecording -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        modifier = Modifier.size(72.dp)
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
            modifier = Modifier.size(36.dp)
        )
    }
}