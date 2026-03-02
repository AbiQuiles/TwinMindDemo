package com.aquiles.twinminddemo.util

import android.content.Context
import android.os.StatFs

class StorageChecker(private val context: Context) {
    private val MIN_BYTES = 50L * 1024 * 1024 // 50MB minimum

    fun hasEnoughStorage(): Boolean {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBytes > MIN_BYTES
    }
}