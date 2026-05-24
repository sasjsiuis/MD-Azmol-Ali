package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsAnalysisDao {
    @Query("SELECT * FROM news_analyses ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<NewsAnalysis>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: NewsAnalysis): Long

    @Delete
    suspend fun deleteAnalysis(analysis: NewsAnalysis)

    @Query("DELETE FROM news_analyses")
    suspend fun deleteAll()
}
