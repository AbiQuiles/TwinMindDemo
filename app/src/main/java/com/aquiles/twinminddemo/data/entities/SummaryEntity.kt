package com.aquiles.twinminddemo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val title: String = "",
    val summary: String = "",
    val actionItems: String = "",
    val keyPoints: String = "",
    val status: String = "PENDING",
    val errorMessage: String? = null
)
