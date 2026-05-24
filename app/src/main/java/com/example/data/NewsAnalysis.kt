package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_analyses")
data class NewsAnalysis(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transcript: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Headline Categories
    val hardNews1: String,
    val hardNews2: String,
    
    val directQuote1: String,
    val directQuote2: String,
    
    val warningAction1: String,
    val warningAction2: String,
    
    val politicalConflict1: String,
    val politicalConflict2: String,
    
    val curiosityQuestion1: String,
    val curiosityQuestion2: String,
    
    // Editor's Remarks
    val editorAgenda: String,
    val editorPublicImpact: String,
    val editorPowerWordsJoined: String
)
