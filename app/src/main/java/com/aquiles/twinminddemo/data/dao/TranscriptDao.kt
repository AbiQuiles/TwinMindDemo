package com.aquiles.twinminddemo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aquiles.twinminddemo.data.entities.TranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcript: TranscriptEntity)

    @Query("SELECT * FROM transcripts WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    fun getTranscriptForSession(sessionId: String): Flow<List<TranscriptEntity>>

    @Query("SELECT text FROM transcripts WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    suspend fun getOrderedTexts(sessionId: String): List<String>
}