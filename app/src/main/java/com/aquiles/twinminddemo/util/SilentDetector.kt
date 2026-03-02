package com.aquiles.twinminddemo.util

import android.media.MediaRecorder

class SilenceDetector {
    fun isSilent(recorder: MediaRecorder?): Boolean {
        val amplitude = recorder?.maxAmplitude ?: return true
        return amplitude < 500
    }
}