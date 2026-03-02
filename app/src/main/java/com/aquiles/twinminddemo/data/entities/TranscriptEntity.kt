package com.aquiles.twinminddemo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val chunkId: String,
    val chunkIndex: Int,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)
