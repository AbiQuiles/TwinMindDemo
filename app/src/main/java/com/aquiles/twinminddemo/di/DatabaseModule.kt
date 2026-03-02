package com.aquiles.twinminddemo.di

import android.content.Context
import androidx.room.Room
import com.aquiles.twinminddemo.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java, "meeting_recorder.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChunkDao(db: AppDatabase) = db.audioChunkDao()

    @Provides
    fun provideTranscriptDao(db: AppDatabase) = db.transcriptDao()

    @Provides
    fun provideSummaryDao(db: AppDatabase) = db.summaryDao()
}
