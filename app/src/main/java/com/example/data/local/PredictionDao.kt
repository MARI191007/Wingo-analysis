package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PredictionResult
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {
    @Query("SELECT * FROM prediction_results WHERE gameMode = :mode ORDER BY timestamp DESC LIMIT 50")
    fun getRecentPredictions(mode: String): Flow<List<PredictionResult>>

    @Query("SELECT * FROM prediction_results WHERE targetPeriodId = :targetPeriodId LIMIT 1")
    suspend fun getPredictionForPeriod(targetPeriodId: String): PredictionResult?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionResult)

    @Update
    suspend fun updatePrediction(prediction: PredictionResult)

    @Query("SELECT * FROM prediction_results WHERE isWin IS NOT NULL ORDER BY timestamp DESC LIMIT 100")
    fun getVerifiedPredictions(): Flow<List<PredictionResult>>

    @Query("DELETE FROM prediction_results")
    suspend fun clearPredictions()
}
