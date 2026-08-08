package com.example.data.repository

import com.example.data.local.PeriodDao
import com.example.data.local.PredictionDao
import com.example.data.model.PeriodRecord
import com.example.data.model.PredictionResult
import com.example.data.remote.WingoRemoteDataSource
import com.example.ml.MlPredictionOutput
import com.example.ml.WingoMlEngine
import com.example.util.PeriodUtils
import kotlinx.coroutines.flow.Flow
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

    fun getVerifiedPredictions(gameMode: String): Flow<List<PredictionResult>> {
        return predictionDao.getVerifiedPredictionsForMode(gameMode)
    }

    suspend fun seedBaselinePeriodHistory(gameMode: String): List<PeriodRecord> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val existing = periodDao.getRecentPeriodsList(gameMode, 5)
        if (existing.size >= 5) {
            return@withContext periodDao.getRecentPeriodsList(gameMode, 500)
        }

        val serverInfo = calculateCurrentServerPeriod(gameMode)
        val activePeriodLong = serverInfo.currentPeriodId.filter { it.isDigit() }.toLongOrNull() ?: 20260808010001L
        val closedBasePeriod = activePeriodLong - 1L

        val seededList = mutableListOf<PeriodRecord>()
        val nowMs = System.currentTimeMillis()
        val intervalMs = serverInfo.intervalSeconds * 1000L
        val modeHash = gameMode.hashCode().toLong()

        for (i in 0 until 50) {
            val periodIdStr = PeriodUtils.normalizePeriodId((closedBasePeriod - i).toString(), gameMode)
            val pLong = periodIdStr.filter { it.isDigit() }.toLongOrNull() ?: (closedBasePeriod - i)
            var z = pLong xor ((i + 1L) * -7046029254386353131L) xor modeHash
            z = (z xor (z ushr 30)) * -4658895280553760867L
            z = (z xor (z ushr 27)) * -7723592293110705685L
            z = z xor (z ushr 31)
            val num = (kotlin.math.abs(z) % 10).toInt()
            val bs = if (num >= 5) "BIG" else "SMALL"
            val col = remoteDataSource.getDigitColor(num)
            seededList.add(
                PeriodRecord(
                    periodId = periodIdStr,
                    gameMode = gameMode,
                    number = num,
                    bigSmall = bs,
                    color = col,
                    timestamp = nowMs - (i * intervalMs),
                    isRealVerified = false
                )
            )
        }

        periodDao.insertPeriods(seededList)
        seededList
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
            val normalizedOnline = onlineRecords.map { record ->
                val normId = PeriodUtils.normalizePeriodId(record.periodId, gameMode)
                record.copy(
                    periodId = normId,
                    gameMode = gameMode,
                    isRealVerified = true
                )
            }
            periodDao.insertPeriods(normalizedOnline)
            
            // Re-verify existing predictions against synced actual records
            val predictionsList = predictionDao.getAllPredictionsList()
            normalizedOnline.forEach { record ->
                val normRecordId = record.periodId
                val suffix = normRecordId.takeLast(4)

                predictionsList.filter { pred ->
                    pred.gameMode == gameMode && (
                        pred.targetPeriodId == normRecordId ||
                        pred.targetPeriodId.endsWith(suffix) ||
                        PeriodUtils.normalizePeriodId(pred.targetPeriodId, gameMode) == normRecordId
                    )
                }.forEach { prediction ->
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

        // Check DB for recent periods
        val existing = periodDao.getRecentPeriodsList(gameMode, 1000)
        val serverInfo = calculateCurrentServerPeriod(gameMode, customBasePeriodId, customSetTimeMs)
        val activePeriodNorm = PeriodUtils.normalizePeriodId(serverInfo.currentPeriodId, gameMode)
        val activePeriodLong = activePeriodNorm.filter { it.isDigit() }.toLongOrNull()

        if (existing.isEmpty()) {
            seedBaselinePeriodHistory(gameMode)
        } else if (activePeriodLong != null) {
            val existingIds = existing.map { PeriodUtils.normalizePeriodId(it.periodId, gameMode) }.toSet()
            val existingSuffixes = existing.map { it.periodId.takeLast(4) }.toSet()
            val latestInDb = existing.maxOfOrNull { PeriodUtils.normalizePeriodId(it.periodId, gameMode).filter { c -> c.isDigit() }.toLongOrNull() ?: 0L } ?: 0L
            val closedPeriodLong = activePeriodLong - 1L

            if (latestInDb > 0L && latestInDb < closedPeriodLong) {
                val maxCatchupCount = 50L
                val startId = maxOf(latestInDb + 1L, closedPeriodLong - maxCatchupCount)
                
                val newRecords = mutableListOf<PeriodRecord>()
                for (id in startId..closedPeriodLong) {
                    val periodIdStr = PeriodUtils.normalizePeriodId(id.toString(), gameMode)
                    val suffix = periodIdStr.takeLast(4)
                    if (!existingIds.contains(periodIdStr) && !existingSuffixes.contains(suffix)) {
                        val digit = remoteDataSource.getOnlineServerDigitForPeriod(periodIdStr, gameMode)
                        val bs = if (digit >= 5) "BIG" else "SMALL"
                        val col = remoteDataSource.getDigitColor(digit)
                        newRecords.add(
                            PeriodRecord(
                                periodId = periodIdStr,
                                gameMode = gameMode,
                                number = digit,
                                bigSmall = bs,
                                color = col,
                                timestamp = System.currentTimeMillis() - ((closedPeriodLong - id) * serverInfo.intervalSeconds * 1000L),
                                isRealVerified = false
                            )
                        )
                    }
                }
                if (newRecords.isNotEmpty()) {
                    periodDao.insertPeriods(newRecords)
                }
            }
            periodDao.getRecentPeriodsList(gameMode, 1000)
        } else {
            existing
        }
    }

    suspend fun ingestYaarwinLiveHistory(
        records: List<PeriodRecord>
    ): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (records.isNotEmpty()) {
            val gameMode = records.firstOrNull()?.gameMode ?: "1Min"
            val verifiedRecords = mutableListOf<PeriodRecord>()
            
            records.forEach { incoming ->
                val normId = PeriodUtils.normalizePeriodId(incoming.periodId, gameMode)
                val verified = incoming.copy(
                    periodId = normId,
                    gameMode = gameMode,
                    isRealVerified = true
                )
                verifiedRecords.add(verified)
            }

            periodDao.insertPeriods(verifiedRecords)

            // Verify ALL matching predictions against incoming verified live records
            val predictionsList = predictionDao.getAllPredictionsList()
            verifiedRecords.forEach { record ->
                val normRecordId = record.periodId
                val suffix = normRecordId.takeLast(4)

                predictionsList.filter { pred ->
                    pred.gameMode == gameMode && (
                        pred.targetPeriodId == normRecordId ||
                        pred.targetPeriodId.endsWith(suffix) ||
                        PeriodUtils.normalizePeriodId(pred.targetPeriodId, gameMode) == normRecordId
                    )
                }.forEach { prediction ->
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

    suspend fun batchImportHistoryText(
        rawText: String,
        gameMode: String
    ): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (rawText.isBlank()) return@withContext 0
        val importedList = mutableListOf<PeriodRecord>()
        
        val pattern = Regex("""\*?(\d{4,20})[\s\S]*?([0-9])""")
        pattern.findAll(rawText).forEach { match ->
            val rawId = match.groupValues[1]
            val num = match.groupValues[2].toIntOrNull()
            if (rawId.length >= 4 && num != null && num in 0..9) {
                val normId = PeriodUtils.normalizePeriodId(rawId, gameMode)
                val bs = if (num >= 5) "BIG" else "SMALL"
                val col = remoteDataSource.getDigitColor(num)
                importedList.add(
                    PeriodRecord(
                        periodId = normId,
                        gameMode = gameMode,
                        number = num,
                        bigSmall = bs,
                        color = col,
                        timestamp = System.currentTimeMillis(),
                        isRealVerified = true
                    )
                )
            }
        }

        if (importedList.isNotEmpty()) {
            ingestYaarwinLiveHistory(importedList)
        }
        importedList.size
    }

    suspend fun ingestUserProvided20Results(
        digits: List<Int>,
        gameMode: String,
        customBasePeriodId: String? = null,
        customSetTimeMs: Long = 0L
    ): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (digits.isEmpty()) return@withContext 0

        val serverInfo = calculateCurrentServerPeriod(gameMode, customBasePeriodId, customSetTimeMs)
        val activePeriodNorm = PeriodUtils.normalizePeriodId(serverInfo.currentPeriodId, gameMode)
        val activePeriodLong = activePeriodNorm.filter { it.isDigit() }.toLongOrNull() ?: 20260808010001L
        val closedBasePeriod = activePeriodLong - 1L

        val records = mutableListOf<PeriodRecord>()
        digits.take(20).forEachIndexed { index, num ->
            val periodIdStr = PeriodUtils.normalizePeriodId((closedBasePeriod - index).toString(), gameMode)
            val bs = if (num >= 5) "BIG" else "SMALL"
            val col = remoteDataSource.getDigitColor(num)
            records.add(
                PeriodRecord(
                    periodId = periodIdStr,
                    gameMode = gameMode,
                    number = num,
                    bigSmall = bs,
                    color = col,
                    timestamp = System.currentTimeMillis() - (index * 60000L),
                    isRealVerified = true
                )
            )
        }

        periodDao.clearPeriods(gameMode)
        periodDao.insertPeriods(records)

        // Verify predictions matching user provided digits
        val predictionsList = predictionDao.getAllPredictionsList()
        records.forEach { record ->
            val normRecordId = record.periodId
            val suffix = normRecordId.takeLast(4)

            predictionsList.filter { pred ->
                pred.gameMode == gameMode && (
                    pred.targetPeriodId == normRecordId ||
                    pred.targetPeriodId.endsWith(suffix) ||
                    PeriodUtils.normalizePeriodId(pred.targetPeriodId, gameMode) == normRecordId
                )
            }.forEach { prediction ->
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

        records.size
    }

    suspend fun initializeDefaultData(gameMode: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val existing = periodDao.getRecentPeriodsList(gameMode, 10)
        if (existing.size < 10) {
            val synced = syncOnlinePeriodHistory(gameMode)
            val updatedExisting = periodDao.getRecentPeriodsList(gameMode, 10)
            if (updatedExisting.isEmpty()) {
                seedBaselinePeriodHistory(gameMode)
            }
        }
    }

    fun getDigitForPeriod(periodId: String, gameMode: String): Int {
        return remoteDataSource.getOnlineServerDigitForPeriod(periodId, gameMode)
    }

    suspend fun fetchLiveServerPeriod(
        gameMode: String
    ): ServerPeriodInfo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val calcInfo = calculateCurrentServerPeriod(gameMode)
        val recentInDb = periodDao.getRecentPeriodsList(gameMode, 1).firstOrNull()

        if (recentInDb != null && !recentInDb.periodId.isNullOrBlank()) {
            val recentNum = recentInDb.periodId.filter { it.isDigit() }.toLongOrNull()
            val calcNum = calcInfo.currentPeriodId.filter { it.isDigit() }.toLongOrNull()
            if (recentNum != null && calcNum != null && calcNum <= recentNum) {
                val adjustedCurr = PeriodUtils.normalizePeriodId(incrementPeriodId(recentInDb.periodId, 1), gameMode)
                val adjustedNext = PeriodUtils.normalizePeriodId(incrementPeriodId(recentInDb.periodId, 2), gameMode)
                return@withContext calcInfo.copy(
                    currentPeriodId = adjustedCurr,
                    nextPeriodId = adjustedNext
                )
            }
        }

        calcInfo
    }

    fun calculateCurrentServerPeriod(
        gameMode: String,
        customBasePeriodId: String? = null,
        customSetTimeMs: Long = 0L
    ): ServerPeriodInfo {
        val now = System.currentTimeMillis()
        val intervalSeconds = when (gameMode) {
            "3Min" -> 180
            "5Min" -> 300
            "10Min" -> 600
            "30s" -> 30
            else -> 60
        }

        val secondsRemaining = (intervalSeconds - ((now / 1000) % intervalSeconds)).toInt().coerceIn(1, intervalSeconds)

        val (currentPeriodId, nextPeriodId) = if (!customBasePeriodId.isNullOrBlank() && customSetTimeMs > 0) {
            val elapsedSeconds = (now - customSetTimeMs) / 1000
            val elapsedIntervals = (elapsedSeconds / intervalSeconds)
            val currRaw = incrementPeriodId(customBasePeriodId, elapsedIntervals)
            val nxtRaw = incrementPeriodId(customBasePeriodId, elapsedIntervals + 1)
            val curr = PeriodUtils.normalizePeriodId(currRaw, gameMode)
            val nxt = PeriodUtils.normalizePeriodId(nxtRaw, gameMode)
            Pair(curr, nxt)
        } else {
            val currLong = remoteDataSource.parsePeriodToLong(null, gameMode, now, intervalSeconds)
            val curr = PeriodUtils.normalizePeriodId(currLong.toString(), gameMode)
            val nxt = PeriodUtils.normalizePeriodId((currLong + 1L).toString(), gameMode)
            Pair(curr, nxt)
        }

        return ServerPeriodInfo(
            currentPeriodId = currentPeriodId,
            nextPeriodId = nextPeriodId,
            secondsRemaining = secondsRemaining,
            intervalSeconds = intervalSeconds,
            gameMode = gameMode
        )
    }

    fun incrementPeriodId(baseId: String, incrementBy: Long): String {
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
    ): PredictionResult? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val periodHistory = periodDao.getRecentPeriodsList(gameMode, 500)
        if (periodHistory.isEmpty()) return@withContext null

        val serverInfo = calculateCurrentServerPeriod(gameMode, customBasePeriodId, customSetTimeMs)
        val targetPeriod = PeriodUtils.normalizePeriodId(serverInfo.currentPeriodId, gameMode)

        val mlOutput: MlPredictionOutput = mlEngine.analyzeAndPredict(
            history = periodHistory,
            algorithm = algorithm,
            targetPeriodId = targetPeriod
        ) ?: return@withContext null

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
            sampleSizeAnalyzed = periodHistory.size,
            timestamp = System.currentTimeMillis()
        )

        predictionDao.insertPrediction(prediction)
        prediction
    }

    fun getOnlineServerDigitForPeriod(periodId: String, gameMode: String): Int {
        return remoteDataSource.getOnlineServerDigitForPeriod(periodId, gameMode)
    }

    suspend fun ingestServerPeriodResult(
        periodId: String,
        number: Int,
        gameMode: String,
        isRealVerified: Boolean = true
    ): PeriodRecord {
        val normId = PeriodUtils.normalizePeriodId(periodId, gameMode)
        val bs = if (number >= 5) "BIG" else "SMALL"
        val color = remoteDataSource.getDigitColor(number)

        val newRecord = PeriodRecord(
            periodId = normId,
            gameMode = gameMode,
            number = number,
            bigSmall = bs,
            color = color,
            timestamp = System.currentTimeMillis(),
            isRealVerified = isRealVerified
        )

        periodDao.insertPeriod(newRecord)

        val suffix = normId.takeLast(4)
        val predictionsList = predictionDao.getAllPredictionsList()
        predictionsList.filter { pred ->
            pred.gameMode == gameMode && (
                pred.targetPeriodId == normId ||
                pred.targetPeriodId.endsWith(suffix) ||
                PeriodUtils.normalizePeriodId(pred.targetPeriodId, gameMode) == normId
            )
        }.forEach { prediction ->
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
