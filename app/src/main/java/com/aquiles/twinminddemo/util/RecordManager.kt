package com.aquiles.twinminddemo.util

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

class AudioFocusManager(
    context: Context,
    private val onFocusLost: () -> Unit,
    private val onFocusGained: () -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @RequiresApi(Build.VERSION_CODES.O)
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setOnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> onFocusLost()
                AudioManager.AUDIOFOCUS_GAIN -> onFocusGained()
            }
        }.build()

    @RequiresApi(Build.VERSION_CODES.O)
    fun requestFocus() { audioManager.requestAudioFocus(focusRequest) }

    @RequiresApi(Build.VERSION_CODES.O)
    fun abandonFocus() { audioManager.abandonAudioFocusRequest(focusRequest) }
}