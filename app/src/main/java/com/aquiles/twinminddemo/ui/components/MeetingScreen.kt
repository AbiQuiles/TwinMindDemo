package com.aquiles.twinminddemo.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.aquiles.twinminddemo.ui.components.record.RecordingView
import com.aquiles.twinminddemo.ui.components.record.RecordingViewModel
import com.aquiles.twinminddemo.ui.components.summary.SummaryView
import com.aquiles.twinminddemo.ui.components.summary.SummaryViewModel

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MeetingScreen() {
    val recordingViewModel: RecordingViewModel = hiltViewModel()
    val summaryViewModel: SummaryViewModel = hiltViewModel()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SummaryView(
                modifier = Modifier.weight(1f),
                viewModel = summaryViewModel
            )

            HorizontalDivider()

            // Recording controls anchored to the bottom
            RecordingView(
                modifier = Modifier.wrapContentHeight(),
                viewModel = recordingViewModel,
                onStopRecording = { summaryViewModel.generateSummary() }
            )
        }
    }
}