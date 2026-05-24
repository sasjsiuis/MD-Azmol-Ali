package com.example.data

import kotlinx.coroutines.flow.Flow

class NewsAnalysisRepository(private val dao: NewsAnalysisDao) {
    val allAnalyses: Flow<List<NewsAnalysis>> = dao.getAllAnalyses()

    suspend fun insert(analysis: NewsAnalysis): Long {
        return dao.insertAnalysis(analysis)
    }

    suspend fun delete(analysis: NewsAnalysis) {
        dao.deleteAnalysis(analysis)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
