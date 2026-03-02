package com.aquiles.twinminddemo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "audio_chunks")
data class AudioChunkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val chunkIndex: Int,
    val filePath: String,
    val durationMs: Long,
    val transcriptionStatus: String = "PENDING",
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
