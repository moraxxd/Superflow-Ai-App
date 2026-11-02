package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voiceflow_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val originalText: String,
    val detectedLanguage: String,
    val confidence: Float,
    val translatedText: String,
    val rewrittenText: String,
    val toneStyle: String,
    val audioDurationMs: Long = 0,
    val modelUsed: String = "Whisper-Local + FastText + NLLB-200"
)
