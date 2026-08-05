package com.example.data.repository

import com.example.data.local.PeriodDao
import com.example.data.local.PredictionDao
import com.example.data.model.PeriodRecord
import com.example.data.model.PredictionResult
import com.example.data.remote.WingoRemoteDataSource
import com.example.ml.MlPredictionOutput
import com.example.ml.WingoMlEngine
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WingoRepository(
    private val periodDao: PeriodDao,
    private val predictionDao: PredictionDao,
    private val mlEngine: WingoMlEngine = WingoMlEngine(),
    private val remoteDataSource: WingoRemoteDataSource = WingoRemoteDataSource()
) {

    fun getPeriodHistory(gameMode: String): Flow<List<PeriodRecord>> {
        return periodDao.getRecentPeriods(gameMode, 500)
    }

    fun getRecentPredictions(gameMode: String): Flow<List<PredictionResult>> {
        return predictionDao.getRecentPredictions(gameMode)
    }

    fun getVerifiedPredictions(): Flow<List<PredictionResult>> {
        return predictionDao.getVerifiedPredictions()
    }

    suspend fun syncOnlinePeriodHistory(
        gameMode: String,
        customBasePeriodId: String? = null,
        customSetTimeMs: Long = 0L
    ): List<PeriodRecord> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val onlineRecords = remoteDataSource.fetchOnlinePeriodHistory(
            gameMode = gameMode,
            count = 500,
            customBasePeriodId = customBasePeriodId,
            customSetTimeMs = customSetTimeMs
        )
        if (onlineRecords.isNotEmpty()) {
            periodDao.insertPeriods(onlineRecords)
            
            // Re-verify existing predictions against synced actual records
            onlineRecords.forEach { record ->
                val prediction = predictionDao.getPredictionForPeriod(record.periodId)
                if (prediction != null && prediction.actualNumber == null) {
                    val isWin = (prediction.predictedBigSmall == record.bigSmall) ||
                            (prediction.primaryNumber == record.number || prediction.secondaryNumber == record.number)
                    val updated = prediction.copy(
                        actualNumber = record.number,
                        actualBigSmall = record.bigSmall,
                        isWin = isWin
                    )
                    predictionDao.updatePrediction(updated)
                }
            }
        }
        onlineRecords
    }

    suspend fun ingestYaarwinLiveHistory(
        records: List<PeriodRecord>
    ): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (records.isNotEmpty()) {
            periodDao.insertPeriods(records)
            records.forEach { record ->
                val prediction = predictionDao.getPredictionForPeriod(record.periodId)
                if (prediction != null && prediction.actualNumber == null) {
                    val isWin = (prediction.predictedBigSmall == record.bigSmall) ||
                            (prediction.primaryNumber == record.number || prediction.secondaryNumber == record.number)
                    val updated = prediction.copy(
                        actualNumber = record.number,
                        actualBigSmall = record.bigSmall,
                        isWin = isWin
                    )
                    predictionDao.updatePrediction(updated)
                }
            }
        }
        records.size
    }

    suspend fun initializeDefaultData(gameMode: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val existing = periodDao.getRecentPeriodsList(gameMode, 10)
        if (existing.size < 100) {
            syncOnlinePeriodHistory(gameMode)
        }
    }

    fun getDigitForPeriod(periodId: String, gameMode: String): Int {
        return remoteDataSource.getOnlineServerDigitForPeriod(periodId, gameMode)
    }

    // Server Period ID Calculator based on game mode & UTC epoch timestamp or user offset
    fun calculateCurrentServerPeriod(
        gameMode: String,
        customBasePeriodId: String? = null,
        customSetTimeMs: Long = 0L
    ): ServerPeriodInfo {
        val now = System.currentTimeMillis()
        val intervalSeconds = when (gameMode) {
            "1Min" -> 60
            "3Min" -> 180
            "5Min" -> 300
            "10Min" -> 600
            else -> 60
        }

        val secondsRemaining = intervalSeconds - ((now / 1000) % intervalSeconds)

        val (currentPeriodId, nextPeriodId) = if (!customBasePeriodId.isNullOrBlank() && customSetTimeMs > 0) {
            val elapsedSeconds = (now - customSetTimeMs) / 1000
            val elapsedIntervals = (elapsedSeconds / intervalSeconds)
            val curr = incrementPeriodId(customBasePeriodId, elapsedIntervals)
            val nxt = incrementPeriodId(customBasePeriodId, elapsedIntervals + 1)
            Pair(curr, nxt)
        } else {
            val currLong = remoteDataSource.parsePeriodToLong(null, gameMode, now, intervalSeconds)
            val curr = currLong.toString()
            val nxt = (currLong + 1L).toString()
            Pair(curr, nxt)
        }

        return ServerPeriodInfo(
            currentPeriodId = currentPeriodId,
            nextPeriodId = nextPeriodId,
            secondsRemaining = secondsRemaining.toInt(),
            intervalSeconds = intervalSeconds,
            gameMode = gameMode
        )
    }

    private fun incrementPeriodId(baseId: String, incrementBy: Long): String {
        if (baseId.isEmpty()) return ""
        val regex = "(\\d+)$".toRegex()
        val match = regex.find(baseId)
        if (match != null) {
            val numStr = match.value
            val numLen = numStr.length
            val numVal = numStr.toLongOrNull() ?: 0L
            val newVal = numVal + incrementBy
            val formattedNum = String.format(Locale.US, "%0${numLen}d", newVal)
            return baseId.substring(0, baseId.length - numLen) + formattedNum
        }
        return "$baseId$incrementBy"
    }

    suspend fun runMlPrediction(
        gameMode: String,
        algorithm: WingoMlEngine.AlgorithmType,
        customBasePeriodId: String? = null,
        customSetTimeMs: Long = 0L
    ): PredictionResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val periodHistory = periodDao.getRecentPeriodsList(gameMode, 500)
        val serverInfo = calculateCurrentServerPeriod(gameMode, customBasePeriodId, customSetTimeMs)
        val targetPeriod = serverInfo.currentPeriodId

        val mlOutput: MlPredictionOutput = mlEngine.analyzeAndPredict(
            history = periodHistory,
            algorithm = algorithm,
            targetPeriodId = targetPeriod
        )

        val prediction = PredictionResult(
            targetPeriodId = targetPeriod,
            gameMode = gameMode,
            predictedBigSmall = mlOutput.bigSmallPrediction,
            bigSmallConfidence = mlOutput.bigSmallConfidence,
            primaryNumber = mlOutput.primaryNumber,
            primaryProbability = mlOutput.primaryProbability,
            secondaryNumber = mlOutput.secondaryNumber,
            secondaryProbability = mlOutput.secondaryProbability,
            predictedColor = mlOutput.predictedColor,
            mlAlgorithm = mlOutput.algorithmName,
            sampleSizeAnalyzed = periodHistory.size.coerceAtLeast(500),
            timestamp = System.currentTimeMillis()
        )

        predictionDao.insertPrediction(prediction)
        prediction
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
