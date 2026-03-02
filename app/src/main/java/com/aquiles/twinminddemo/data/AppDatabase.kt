package com.aquiles.twinminddemo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aquiles.twinminddemo.data.dao.AudioChunkDao
import com.aquiles.twinminddemo.data.dao.SummaryDao
import com.aquiles.twinminddemo.data.dao.TranscriptDao
import com.aquiles.twinminddemo.data.entities.AudioChunkEntity
import com.aquiles.twinminddemo.data.entities.SummaryEntity
import com.aquiles.twinminddemo.data.entities.TranscriptEntity

@Database(
    entities = [AudioChunkEntity::class, TranscriptEntity::class, SummaryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun summaryDao(): SummaryDao
}