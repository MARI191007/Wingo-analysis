package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction_results")
data class PredictionResult(
    @PrimaryKey val targetPeriodId: String,
    val gameMode: String,
    val predictedBigSmall: String, // "BIG" or "SMALL"
    val bigSmallConfidence: Float, // e.g. 84.5f
    val primaryNumber: Int, // e.g. 7
    val primaryProbability: Float, // e.g. 42.0f
    val secondaryNumber: Int, // e.g. 3
    val secondaryProbability: Float, // e.g. 35.5f
    val predictedColor: String, // "GREEN", "RED", or "VIOLET"
    val mlAlgorithm: String, // "Markov Chain Neural", "Trend Momentum", "Hybrid AI Ensemble"
    val sampleSizeAnalyzed: Int, // e.g. 50
    val timestamp: Long = System.currentTimeMillis(),
    val actualNumber: Int? = null,
    val actualBigSmall: String? = null,
    val isWin: Boolean? = null
)
