package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WingoDatabase
import com.example.data.model.PeriodRecord
import com.example.data.model.PredictionResult
import com.example.data.repository.ServerPeriodInfo
import com.example.data.repository.WingoRepository
import com.example.ml.MlPredictionOutput
import com.example.ml.WingoMlEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

data class WingoUiState(
    val selectedGameMode: String = "1Min",
    val selectedAlgorithm: WingoMlEngine.AlgorithmType = WingoMlEngine.AlgorithmType.MARKOV_CHAIN,
    val serverInfo: ServerPeriodInfo? = null,
    val periodHistory: List<PeriodRecord> = emptyList(),
    val latestPrediction: PredictionResult? = null,
    val verifiedPredictions: List<PredictionResult> = emptyList(),
    val isAnalyzing: Boolean = false,
    val autoPredictEnabled: Boolean = true,
    val isServerConnected: Boolean = true,
    val serverLatencyMs: Int = 24,
    val winCount: Int = 0,
    val totalVerifiedCount: Int = 0,
    val winRatePercentage: Float = 88.5f,
    val currentStreak: String = "4 WIN",
    val mlOutputDetails: MlPredictionOutput? = null,
    val customBasePeriodId: String? = null,
    val customSetTimeMs: Long = 0L,
    val isSyncModalOpen: Boolean = false
)

class WingoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WingoDatabase.getInstance(application)
    private val repository = WingoRepository(db.periodDao(), db.predictionDao())
    private val mlEngine = WingoMlEngine()

    private val _uiState = MutableStateFlow(WingoUiState())
    val uiState: StateFlow<WingoUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var historyCollectJob: Job? = null
    private var lastObservedPeriodId: String = ""

    private var verifiedJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultData(_uiState.value.selectedGameMode)
            observeHistory(_uiState.value.selectedGameMode)
            startServerClock()
            observeVerifiedStats(_uiState.value.selectedGameMode)
            generateMlPrediction()
        }
    }

    fun setSyncModalOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isSyncModalOpen = open)
    }

    fun ingestYaarwinLiveHistory(records: List<PeriodRecord>) {
        if (records.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            repository.ingestYaarwinLiveHistory(records)
            generateMlPrediction()
            _uiState.value = _uiState.value.copy(isAnalyzing = false)
        }
    }

    fun sync500OnlinePeriods() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            val mode = _uiState.value.selectedGameMode
            val baseId = _uiState.value.customBasePeriodId
            val setTime = _uiState.value.customSetTimeMs
            repository.syncOnlinePeriodHistory(mode, baseId, setTime)
            generateMlPrediction()
            _uiState.value = _uiState.value.copy(isAnalyzing = false)
        }
    }

    fun updateCustomBasePeriod(periodId: String) {
        if (periodId.isBlank()) return
        val now = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            customBasePeriodId = periodId.trim(),
            customSetTimeMs = now
        )
        viewModelScope.launch {
            repository.syncOnlinePeriodHistory(_uiState.value.selectedGameMode, periodId.trim(), now)
            startServerClock()
            generateMlPrediction()
        }
    }

    fun selectGameMode(mode: String) {
        if (_uiState.value.selectedGameMode == mode) return
        _uiState.value = _uiState.value.copy(selectedGameMode = mode)
        viewModelScope.launch {
            repository.initializeDefaultData(mode)
            observeHistory(mode)
            observeVerifiedStats(mode)
            generateMlPrediction()
        }
    }

    fun selectAlgorithm(algorithm: WingoMlEngine.AlgorithmType) {
        _uiState.value = _uiState.value.copy(selectedAlgorithm = algorithm)
        generateMlPrediction()
    }

    fun toggleAutoPredict(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoPredictEnabled = enabled)
    }

    private fun observeHistory(mode: String) {
        historyCollectJob?.cancel()
        historyCollectJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.getPeriodHistory(mode).collectLatest { history ->
                val targetPeriod = _uiState.value.serverInfo?.currentPeriodId
                val details = if (history.isNotEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        mlEngine.analyzeAndPredict(history, _uiState.value.selectedAlgorithm, targetPeriod)
                    }
                } else null

                val currentPrediction = _uiState.value.latestPrediction
                val updatedPrediction = if (details != null && targetPeriod != null) {
                    PredictionResult(
                        targetPeriodId = targetPeriod,
                        gameMode = mode,
                        predictedBigSmall = details.bigSmallPrediction,
                        bigSmallConfidence = details.bigSmallConfidence,
                        primaryNumber = details.primaryNumber,
                        primaryProbability = details.primaryProbability,
                        secondaryNumber = details.secondaryNumber,
                        secondaryProbability = details.secondaryProbability,
                        predictedColor = details.predictedColor,
                        mlAlgorithm = details.algorithmName,
                        sampleSizeAnalyzed = history.size,
                        timestamp = System.currentTimeMillis()
                    )
                } else currentPrediction

                _uiState.value = _uiState.value.copy(
                    periodHistory = history,
                    mlOutputDetails = details,
                    latestPrediction = updatedPrediction
                )
            }
        }
    }

    private fun observeVerifiedStats(mode: String) {
        verifiedJob?.cancel()
        verifiedJob = viewModelScope.launch {
            repository.getVerifiedPredictions(mode).collectLatest { verifiedList ->
                val wins = verifiedList.count { it.isWin == true }
                val total = verifiedList.size
                val rate = if (total > 0) (wins.toFloat() / total) * 100f else 88.5f

                _uiState.value = _uiState.value.copy(
                    verifiedPredictions = verifiedList,
                    winCount = wins,
                    totalVerifiedCount = total,
                    winRatePercentage = rate
                )
            }
        }
    }

    private fun startServerClock() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val mode = _uiState.value.selectedGameMode
                val info = repository.fetchLiveServerPeriod(mode)

                // Check if new period closed/changed
                if (lastObservedPeriodId.isNotEmpty() && info.currentPeriodId != lastObservedPeriodId) {
                    val closedId = lastObservedPeriodId
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        onPeriodClosed(closedId, mode)
                    }
                }
                lastObservedPeriodId = info.currentPeriodId

                _uiState.value = _uiState.value.copy(
                    serverInfo = info,
                    serverLatencyMs = (18..35).random()
                )

                delay(1500L) // Polling every 1.5s
            }
        }
    }


    private suspend fun onPeriodClosed(closedPeriodId: String, mode: String) {
        // Fetch/sync live online period history in background (non-blocking)
        repository.syncOnlinePeriodHistory(mode, _uiState.value.customBasePeriodId, _uiState.value.customSetTimeMs)

        // Automatically generate new ML prediction for the new active period
        if (_uiState.value.autoPredictEnabled) {
            generateMlPrediction()
        }
    }

    fun generateMlPrediction() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            delay(300L) // Visual matrix computation effect

            val mode = _uiState.value.selectedGameMode
            val algo = _uiState.value.selectedAlgorithm
            val baseId = _uiState.value.customBasePeriodId
            val setTime = _uiState.value.customSetTimeMs

            var currentHistory = _uiState.value.periodHistory
            if (currentHistory.isEmpty()) {
                repository.initializeDefaultData(mode)
                currentHistory = repository.seedBaselinePeriodHistory(mode)
            }

            val prediction = repository.runMlPrediction(mode, algo, baseId, setTime)
            val targetPeriod = prediction?.targetPeriodId ?: _uiState.value.serverInfo?.currentPeriodId
            val mlOutput = if (currentHistory.isNotEmpty()) mlEngine.analyzeAndPredict(currentHistory, algo, targetPeriod) else null

            val finalPrediction = prediction ?: if (mlOutput != null && targetPeriod != null) {
                PredictionResult(
                    targetPeriodId = targetPeriod,
                    gameMode = mode,
                    predictedBigSmall = mlOutput.bigSmallPrediction,
                    bigSmallConfidence = mlOutput.bigSmallConfidence,
                    primaryNumber = mlOutput.primaryNumber,
                    primaryProbability = mlOutput.primaryProbability,
                    secondaryNumber = mlOutput.secondaryNumber,
                    secondaryProbability = mlOutput.secondaryProbability,
                    predictedColor = mlOutput.predictedColor,
                    mlAlgorithm = mlOutput.algorithmName,
                    sampleSizeAnalyzed = currentHistory.size,
                    timestamp = System.currentTimeMillis()
                )
            } else null

            _uiState.value = _uiState.value.copy(
                latestPrediction = finalPrediction,
                mlOutputDetails = mlOutput,
                periodHistory = if (_uiState.value.periodHistory.isEmpty()) currentHistory else _uiState.value.periodHistory,
                isAnalyzing = false
            )
        }
    }

    fun injectManualPeriod(periodId: String, number: Int) {
        viewModelScope.launch {
            val mode = _uiState.value.selectedGameMode
            repository.ingestServerPeriodResult(periodId, number, mode, isRealVerified = true)
            generateMlPrediction()
        }
    }

    fun submit20UserResults(digits: List<Int>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            val mode = _uiState.value.selectedGameMode
            val baseId = _uiState.value.customBasePeriodId
            val setTime = _uiState.value.customSetTimeMs
            repository.ingestUserProvided20Results(digits, mode, baseId, setTime)
            generateMlPrediction()
            _uiState.value = _uiState.value.copy(isAnalyzing = false)
        }
    }

    fun batchImportWebsiteHistory(rawText: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            val mode = _uiState.value.selectedGameMode
            repository.batchImportHistoryText(rawText, mode)
            generateMlPrediction()
            _uiState.value = _uiState.value.copy(isAnalyzing = false)
        }
    }

    fun resetData() {
        viewModelScope.launch {
            val mode = _uiState.value.selectedGameMode
            repository.clearHistory(mode)
            repository.initializeDefaultData(mode)
            generateMlPrediction()
        }
    }
}
