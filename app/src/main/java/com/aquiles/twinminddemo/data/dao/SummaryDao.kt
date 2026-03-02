package com.aquiles.twinminddemo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aquiles.twinminddemo.data.entities.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: SummaryEntity)

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    fun getSummaryForSession(sessionId: String): Flow<SummaryEntity?>

    @Query("UPDATE summaries SET title=:title, summary=:summary, actionItems=:actionItems, keyPoints=:keyPoints, status='DONE' WHERE sessionId=:sessionId")
    suspend fun updateContent(sessionId: String, title: String, summary: String, actionItems: String, keyPoints: String)
}