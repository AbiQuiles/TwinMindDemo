package com.aquiles.twinminddemo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aquiles.twinminddemo.data.entities.AudioChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: AudioChunkEntity)

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    fun getChunksForSession(sessionId: String): Flow<List<AudioChunkEntity>>

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    suspend fun getChunksForSessionOnce(sessionId: String): List<AudioChunkEntity>

    @Query("UPDATE audio_chunks SET transcriptionStatus = :status, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM audio_chunks WHERE transcriptionStatus IN ('PENDING','FAILED') AND sessionId = :sessionId")
    suspend fun getPendingChunks(sessionId: String): List<AudioChunkEntity>
}