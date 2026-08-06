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

    suspend fun seedBaselinePeriodHistory(gameMode: String): List<PeriodRecord> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val serverInfo = calculateCurrentServerPeriod(gameMode)
        val activePeriodLong = serverInfo.currentPeriodId.toLongOrNull() ?: 2026080510001000L
        val closedBasePeriod = activePeriodLong - 1L

        val seededList = mutableListOf<PeriodRecord>()
        val nowMs = System.currentTimeMillis()
        val intervalMs = serverInfo.intervalSeconds * 1000L
        val modeHash = gameMode.hashCode().toLong()

        for (i in 0 until 50) {
            val periodIdStr = (closedBasePeriod - i).toString()
            val pLong = periodIdStr.toLongOrNull() ?: (closedBasePeriod - i)
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
            onlineRecords
        } else {
            // Fallback sync: Ensure history is continuously generated up to the current closed period
            val existing = periodDao.getRecentPeriodsList(gameMode, 50)
            val serverInfo = calculateCurrentServerPeriod(gameMode, customBasePeriodId, customSetTimeMs)
            val activePeriodLong = serverInfo.currentPeriodId.toLongOrNull()

            if (existing.isEmpty()) {
                seedBaselinePeriodHistory(gameMode)
            } else if (activePeriodLong != null) {
                val latestInDb = existing.firstOrNull()?.periodId?.toLongOrNull()
                val closedPeriodLong = activePeriodLong - 1L
                if (latestInDb == null || latestInDb < closedPeriodLong) {
                    val startId = if (latestInDb != null && (closedPeriodLong - latestInDb) <= 50) latestInDb + 1L else (closedPeriodLong - 20L)
                    val newRecords = mutableListOf<PeriodRecord>()
                    for (id in startId..closedPeriodLong) {
                        val periodIdStr = id.toString()
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
                                timestamp = System.currentTimeMillis(),
                                isRealVerified = true
                            )
                        )
                    }
                    if (newRecords.isNotEmpty()) {
                        periodDao.insertPeriods(newRecords)
                        // Re-verify predictions for newly closed periods
                        newRecords.forEach { record ->
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
                }
                periodDao.getRecentPeriodsList(gameMode, 50)
            } else {
                existing
            }
        }
    }

    suspend fun ingestYaarwinLiveHistory(
        records: List<PeriodRecord>
    ): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (records.isNotEmpty()) {
            val gameMode = records.firstOrNull()?.gameMode ?: "1Min"
            val existingPeriods = periodDao.getRecentPeriodsList(gameMode, 500)

            val verifiedRecords = mutableListOf<PeriodRecord>()
            records.forEach { incoming ->
                val normId = PeriodUtils.normalizePeriodId(incoming.periodId, gameMode)
                val verified = incoming.copy(
                    periodId = normId,
                    isRealVerified = true
                )
                verifiedRecords.add(verified)

                // Match and replace any estimated record whose ID matches normId or ends with suffix
                val suffix = normId.takeLast(4)
                existingPeriods.forEach { existing ->
                    if (existing.periodId == normId || (suffix.length >= 4 && existing.periodId.endsWith(suffix))) {
                        val updatedExisting = existing.copy(
                            periodId = normId,
                            number = incoming.number,
                            bigSmall = incoming.bigSmall,
                            color = incoming.color,
                            isRealVerified = true
                        )
                        verifiedRecords.add(updatedExisting)
                    }
                }
            }

            periodDao.insertPeriods(verifiedRecords)

            verifiedRecords.forEach { record ->
                val prediction = predictionDao.getPredictionForPeriod(record.periodId)
                if (prediction != null) {
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
        
        // Regex matches lines or strings containing period numbers (*010570 or 20260805100010570) and winning digits (0-9)
        val pattern = Regex("""\*?(\d{4,20})[\s\S]*?([0-9])""")
        pattern.findAll(rawText).forEach { match ->
            val rawId = match.groupValues[1]
            val num = match.groupValues[2].toIntOrNull()
            if (rawId.length >= 4 && num != null && num in 0..9) {
                val normId = PeriodUtils.normalizePeriodId(rawId, gameMode)
                val bs = if (num >= 5) "BIG" else "SMALL"
                val col = when (num) {
                    0, 5 -> "VIOLET"
                    1, 3, 7, 9 -> "GREEN"
                    else -> "RED"
                }
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
        val activePeriodLong = serverInfo.currentPeriodId.toLongOrNull() ?: 2026080510001000L
        val closedBasePeriod = activePeriodLong - 1L

        val records = mutableListOf<PeriodRecord>()
        digits.take(20).forEachIndexed { index, num ->
            val periodIdStr = (closedBasePeriod - index).toString()
            val bs = if (num >= 5) "BIG" else "SMALL"
            val col = when (num) {
                0, 5 -> "VIOLET"
                1, 3, 7, 9 -> "GREEN"
                else -> "RED"
            }
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

        // Clear previous periods for clean user 20-result analysis & insert
        periodDao.clearPeriods(gameMode)
        periodDao.insertPeriods(records)
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
                val adjustedCurr = incrementPeriodId(recentInDb.periodId, 1)
                val adjustedNext = incrementPeriodId(recentInDb.periodId, 2)
                return@withContext calcInfo.copy(
                    currentPeriodId = adjustedCurr,
                    nextPeriodId = adjustedNext
                )
            }
        }

        calcInfo
    }

    // Server Period ID Calculator fallback based on local clock
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
        val targetPeriod = serverInfo.nextPeriodId

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

    // Ingest new server result when a period closes or is updated
    suspend fun ingestServerPeriodResult(
        periodId: String,
        number: Int,
        gameMode: String,
        isRealVerified: Boolean = true
    ): PeriodRecord {
        val normId = PeriodUtils.normalizePeriodId(periodId, gameMode)
        val bs = if (number >= 5) "BIG" else "SMALL"
        val color = when (number) {
            0, 5 -> "VIOLET"
            1, 3, 7, 9 -> "GREEN"
            else -> "RED"
        }

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

        // Also check if any existing period matches or ends with this suffix
        val suffix = normId.takeLast(4)
        val existingList = periodDao.getRecentPeriodsList(gameMode, 500)
        existingList.forEach { existing ->
            if (existing.periodId == normId || (suffix.length >= 4 && existing.periodId.endsWith(suffix))) {
                periodDao.insertPeriod(
                    existing.copy(
                        periodId = normId,
                        number = number,
                        bigSmall = bs,
                        color = color,
                        isRealVerified = isRealVerified
                    )
                )
            }
        }

        // Verify prediction for this period if present
        val prediction = predictionDao.getPredictionForPeriod(normId) ?: predictionDao.getPredictionForPeriod(periodId)
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
