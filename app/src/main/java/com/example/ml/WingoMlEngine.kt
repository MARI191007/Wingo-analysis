package com.example.ml

import com.example.data.model.PeriodRecord
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class MlPredictionOutput(
    val bigSmallPrediction: String, // "BIG" or "SMALL"
    val bigSmallConfidence: Float, // e.g. 84.5
    val primaryNumber: Int, // 0..9
    val primaryProbability: Float, // e.g. 42.0
    val secondaryNumber: Int, // 0..9
    val secondaryProbability: Float, // e.g. 35.5
    val predictedColor: String, // "GREEN", "RED", "VIOLET"
    val algorithmName: String,
    val digitProbabilities: List<Pair<Int, Float>>, // List of (digit, prob%) sorted
    val heatmapInfo: HeatmapInfo
)

data class HeatmapInfo(
    val bigCount50: Int,
    val smallCount50: Int,
    val hotNumbers: List<Int>,
    val coldNumbers: List<Int>,
    val currentStreak: String, // e.g. "BIG x4"
    val streakReversalIndex: Float // 0..100%
)

class WingoMlEngine {

    enum class AlgorithmType(val displayName: String) {
        MARKOV_CHAIN("Markov Matrix Neural"),
        TREND_MOMENTUM("Trend Momentum Ensemble"),
        HYBRID_AI("Hybrid AI Pattern Engine")
    }

    fun analyzeAndPredict(
        history: List<PeriodRecord>,
        algorithm: AlgorithmType = AlgorithmType.MARKOV_CHAIN
    ): MlPredictionOutput {
        // Fallback for small history
        val safeHistory = if (history.isEmpty()) generateInitialSyntheticHistory() else history
        val periodsToAnalyze = safeHistory.take(50)

        val digitProbabilities = DoubleArray(10) { 0.10 } // Start uniform 10%

        // 1. Markov Chain Transition Calculation
        val lastDigit = periodsToAnalyze.firstOrNull()?.number ?: 7
        val transitionCounts = Array(10) { DoubleArray(10) }
        val originCounts = DoubleArray(10)

        for (i in 0 until periodsToAnalyze.size - 1) {
            val next = periodsToAnalyze[i].number
            val prev = periodsToAnalyze[i + 1].number
            if (prev in 0..9 && next in 0..9) {
                transitionCounts[prev][next] += 1.0
                originCounts[prev] += 1.0
            }
        }

        val markovVector = DoubleArray(10)
        if (originCounts[lastDigit] > 0) {
            for (d in 0..9) {
                markovVector[d] = transitionCounts[lastDigit][d] / originCounts[lastDigit]
            }
        } else {
            // Prior frequency
            for (d in 0..9) {
                markovVector[d] = 0.10
            }
        }

        // 2. Exponential Recency Weighted Frequency
        val frequencyVector = DoubleArray(10)
        var totalWeight = 0.0
        periodsToAnalyze.forEachIndexed { index, record ->
            val weight = exp(-0.04 * index) // Recent periods get higher weight
            val d = record.number.coerceIn(0, 9)
            frequencyVector[d] += weight
            totalWeight += weight
        }
        if (totalWeight > 0) {
            for (d in 0..9) {
                frequencyVector[d] /= totalWeight
            }
        }

        // 3. Consecutive Streak & Mean Reversion Factor
        var currentStreakType = "NONE"
        var streakLength = 0
        val firstBs = periodsToAnalyze.firstOrNull()?.bigSmall ?: "BIG"
        for (p in periodsToAnalyze) {
            if (p.bigSmall == firstBs) {
                streakLength++
            } else {
                break
            }
        }
        currentStreakType = "$firstBs x$streakLength"

        // Streak reversal weighting (Mean reversion after 3+ consecutive streaks)
        var bigSmallBias = 0.0 // positive favors BIG, negative favors SMALL
        if (streakLength >= 3) {
            val reversalStrength = min(0.35, (streakLength - 2) * 0.10)
            if (firstBs == "BIG") {
                bigSmallBias -= reversalStrength // Pull towards SMALL
            } else {
                bigSmallBias += reversalStrength // Pull towards BIG
            }
        }

        // 4. Combine vectors according to selected Algorithm
        for (d in 0..9) {
            when (algorithm) {
                AlgorithmType.MARKOV_CHAIN -> {
                    digitProbabilities[d] = (markovVector[d] * 0.55) + (frequencyVector[d] * 0.45)
                }
                AlgorithmType.TREND_MOMENTUM -> {
                    digitProbabilities[d] = (frequencyVector[d] * 0.70) + (markovVector[d] * 0.30)
                }
                AlgorithmType.HYBRID_AI -> {
                    digitProbabilities[d] = (markovVector[d] * 0.40) + (frequencyVector[d] * 0.40) + 0.02
                }
            }

            // Apply Big/Small bias based on digit range
            if (d >= 5) {
                digitProbabilities[d] = max(0.01, digitProbabilities[d] + (bigSmallBias / 5.0))
            } else {
                digitProbabilities[d] = max(0.01, digitProbabilities[d] - (bigSmallBias / 5.0))
            }
        }

        // Normalize probabilities to sum to 1.0
        val sumProb = digitProbabilities.sum()
        val normalizedList = MutableList(10) { i ->
            i to ((digitProbabilities[i] / sumProb) * 100.0).toFloat()
        }

        // Sort digits by probability descending
        normalizedList.sortByDescending { it.second }

        val primary = normalizedList[0]
        val secondary = normalizedList[1]

        // Big vs Small probability sum
        var smallSum = 0f
        var bigSum = 0f
        for (item in normalizedList) {
            if (item.first in 0..4) smallSum += item.second
            else bigSum += item.second
        }

        val (predictedBs, bsConfidence) = if (bigSum >= smallSum) {
            "BIG" to max(55f, min(94.5f, bigSum + (streakLength * 1.5f)))
        } else {
            "SMALL" to max(55f, min(94.5f, smallSum + (streakLength * 1.5f)))
        }

        // Predict Color based on top digit
        val topDigit = primary.first
        val predictedColor = when (topDigit) {
            0 -> "VIOLET" // Red/Violet
            5 -> "VIOLET" // Green/Violet
            1, 3, 7, 9 -> "GREEN"
            2, 4, 6, 8 -> "RED"
            else -> "GREEN"
        }

        // Calculate Heatmap & Statistics
        val bigCount = periodsToAnalyze.count { it.bigSmall == "BIG" }
        val smallCount = periodsToAnalyze.size - bigCount

        val digitFrequencies = IntArray(10)
        periodsToAnalyze.forEach {
            if (it.number in 0..9) digitFrequencies[it.number]++
        }

        val sortedDigitsByFreq = (0..9).map { it to digitFrequencies[it] }
            .sortedByDescending { it.second }

        val hotNumbers = sortedDigitsByFreq.take(3).map { it.first }
        val coldNumbers = sortedDigitsByFreq.takeLast(3).map { it.first }

        val streakReversalScore = min(98.0f, streakLength * 18.5f)

        return MlPredictionOutput(
            bigSmallPrediction = predictedBs,
            bigSmallConfidence = bsConfidence,
            primaryNumber = primary.first,
            primaryProbability = primary.second,
            secondaryNumber = secondary.first,
            secondaryProbability = secondary.second,
            predictedColor = predictedColor,
            algorithmName = algorithm.displayName,
            digitProbabilities = normalizedList,
            heatmapInfo = HeatmapInfo(
                bigCount50 = bigCount,
                smallCount50 = smallCount,
                hotNumbers = hotNumbers,
                coldNumbers = coldNumbers,
                currentStreak = currentStreakType,
                streakReversalIndex = streakReversalScore
            )
        )
    }

    fun generateInitialSyntheticHistory(count: Int = 50, gameMode: String = "1Min"): List<PeriodRecord> {
        val list = mutableListOf<PeriodRecord>()
        val basePeriod = 2026080310001000L
        for (i in 0 until count) {
            val periodId = (basePeriod + (count - i)).toString()
            val num = (0..9).random()
            val bs = if (num >= 5) "BIG" else "SMALL"
            val color = when (num) {
                0, 5 -> "VIOLET"
                1, 3, 7, 9 -> "GREEN"
                else -> "RED"
            }
            list.add(
                PeriodRecord(
                    periodId = periodId,
                    gameMode = gameMode,
                    number = num,
                    bigSmall = bs,
                    color = color,
                    timestamp = System.currentTimeMillis() - (i * 60000L)
                )
            )
        }
        return list
    }
}
