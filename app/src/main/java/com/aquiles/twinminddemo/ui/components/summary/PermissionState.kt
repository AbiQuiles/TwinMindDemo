package com.aquiles.twinminddemo.ui.components.summary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class PermissionState(
    val recordAudioGranted: Boolean,
    val phoneStateGranted: Boolean
) {
    val canRecord: Boolean get() = recordAudioGranted
    val canDetectCalls: Boolean get() = phoneStateGranted

    companion object {
        fun check(context: Context): PermissionState = PermissionState(
            recordAudioGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED,
            phoneStateGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
}