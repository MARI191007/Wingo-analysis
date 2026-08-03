package com.example.data.repository

import com.example.data.local.PeriodDao
import com.example.data.local.PredictionDao
import com.example.data.model.PeriodRecord
import com.example.data.model.PredictionResult
import com.example.ml.MlPredictionOutput
import com.example.ml.WingoMlEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WingoRepository(
    private val periodDao: PeriodDao,
    private val predictionDao: PredictionDao,
    private val mlEngine: WingoMlEngine = WingoMlEngine()
) {

    fun getPeriodHistory(gameMode: String): Flow<List<PeriodRecord>> {
        return periodDao.getRecentPeriods(gameMode, 50)
    }

    fun getRecentPredictions(gameMode: String): Flow<List<PredictionResult>> {
        return predictionDao.getRecentPredictions(gameMode)
    }

    fun getVerifiedPredictions(): Flow<List<PredictionResult>> {
        return predictionDao.getVerifiedPredictions()
    }

    suspend fun initializeDefaultData(gameMode: String) {
        val existing = periodDao.getRecentPeriodsList(gameMode, 1)
        if (existing.isEmpty()) {
            val synthetic = mlEngine.generateInitialSyntheticHistory(50, gameMode)
            periodDao.insertPeriods(synthetic)
        }
    }

    // Server Period ID Calculator based on game mode & current Unix timestamp
    fun calculateCurrentServerPeriod(gameMode: String): ServerPeriodInfo {
        val now = System.currentTimeMillis()
        val intervalSeconds = when (gameMode) {
            "1Min" -> 60
            "3Min" -> 180
            "5Min" -> 300
            "10Min" -> 600
            else -> 60
        }

        val secondsRemaining = intervalSeconds - ((now / 1000) % intervalSeconds)
        val formattedDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
        val currentPeriodIndex = (now / 1000 / intervalSeconds) % 1440 + 1000

        val currentPeriodId = "$formattedDate$currentPeriodIndex"
        val nextPeriodId = "$formattedDate${currentPeriodIndex + 1}"

        return ServerPeriodInfo(
            currentPeriodId = currentPeriodId,
            nextPeriodId = nextPeriodId,
            secondsRemaining = secondsRemaining.toInt(),
            intervalSeconds = intervalSeconds,
            gameMode = gameMode
        )
    }

    suspend fun runMlPrediction(
        gameMode: String,
        algorithm: WingoMlEngine.AlgorithmType
    ): PredictionResult {
        val periodHistory = periodDao.getRecentPeriodsList(gameMode, 50)
        val mlOutput: MlPredictionOutput = mlEngine.analyzeAndPredict(periodHistory, algorithm)
        val serverInfo = calculateCurrentServerPeriod(gameMode)

        val prediction = PredictionResult(
            targetPeriodId = serverInfo.nextPeriodId,
            gameMode = gameMode,
            predictedBigSmall = mlOutput.bigSmallPrediction,
            bigSmallConfidence = mlOutput.bigSmallConfidence,
            primaryNumber = mlOutput.primaryNumber,
            primaryProbability = mlOutput.primaryProbability,
            secondaryNumber = mlOutput.secondaryNumber,
            secondaryProbability = mlOutput.secondaryProbability,
            predictedColor = mlOutput.predictedColor,
            mlAlgorithm = mlOutput.algorithmName,
            sampleSizeAnalyzed = periodHistory.size.coerceAtLeast(50),
            timestamp = System.currentTimeMillis()
        )

        predictionDao.insertPrediction(prediction)
        return prediction
    }

    // Ingest new server result when a period closes
    suspend fun ingestServerPeriodResult(
        periodId: String,
        number: Int,
        gameMode: String
    ): PeriodRecord {
        val bs = if (number >= 5) "BIG" else "SMALL"
        val color = when (number) {
            0, 5 -> "VIOLET"
            1, 3, 7, 9 -> "GREEN"
            else -> "RED"
        }

        val newRecord = PeriodRecord(
            periodId = periodId,
            gameMode = gameMode,
            number = number,
            bigSmall = bs,
            color = color,
            timestamp = System.currentTimeMillis()
        )

        periodDao.insertPeriod(newRecord)

        // Verify prediction for this period if present
        val prediction = predictionDao.getPredictionForPeriod(periodId)
        if (prediction != null) {
            val isWin = (prediction.predictedBigSmall == bs) ||
                    (prediction.primaryNumber == number || prediction.secondaryNumber == number)
            val updated = prediction.copy(
                actualNumber = number,
                actualBigSmall = bs,
                isWin = isWin
            )
            predictionDao.updatePrediction(updated)
        }

        return newRecord
    }

    suspend fun clearHistory(gameMode: String) {
        periodDao.clearPeriods(gameMode)
        predictionDao.clearPredictions()
    }
}

data class ServerPeriodInfo(
    val currentPeriodId: String,
    val nextPeriodId: String,
    val secondsRemaining: Int,
    val intervalSeconds: Int,
    val gameMode: String
)
