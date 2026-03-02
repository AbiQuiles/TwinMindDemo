package com.aquiles.twinminddemo.data.repositories

import com.aquiles.twinminddemo.data.dao.AudioChunkDao
import com.aquiles.twinminddemo.data.entities.AudioChunkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecordingRepository @Inject constructor(
    private val chunkDao: AudioChunkDao,
) {

    suspend fun saveChunk(chunk: AudioChunkEntity) =
        chunkDao.insert(chunk)

    suspend fun updateChunkStatus(id: String, status: String) =
        chunkDao.updateStatus(id, status)
}