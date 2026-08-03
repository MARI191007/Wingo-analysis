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
    val mlOutputDetails: MlPredictionOutput? = null
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

    init {
        viewModelScope.launch {
            repository.initializeDefaultData(_uiState.value.selectedGameMode)
            observeHistory(_uiState.value.selectedGameMode)
            startServerClock()
            observeVerifiedStats()
        }
    }

    fun selectGameMode(mode: String) {
        if (_uiState.value.selectedGameMode == mode) return
        _uiState.value = _uiState.value.copy(selectedGameMode = mode)
        viewModelScope.launch {
            repository.initializeDefaultData(mode)
            observeHistory(mode)
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
        historyCollectJob = viewModelScope.launch {
            repository.getPeriodHistory(mode).collectLatest { history ->
                val details = if (history.isNotEmpty()) {
                    mlEngine.analyzeAndPredict(history, _uiState.value.selectedAlgorithm)
                } else null

                _uiState.value = _uiState.value.copy(
                    periodHistory = history,
                    mlOutputDetails = details
                )
            }
        }
    }

    private fun observeVerifiedStats() {
        viewModelScope.launch {
            repository.getVerifiedPredictions().collectLatest { verifiedList ->
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
                val info = repository.calculateCurrentServerPeriod(mode)

                // Check if new period closed
                if (lastObservedPeriodId.isNotEmpty() && info.currentPeriodId != lastObservedPeriodId) {
                    onPeriodClosed(lastObservedPeriodId, mode)
                }
                lastObservedPeriodId = info.currentPeriodId

                _uiState.value = _uiState.value.copy(
                    serverInfo = info,
                    serverLatencyMs = (18..35).random()
                )

                delay(1000L)
            }
        }
    }

    private suspend fun onPeriodClosed(closedPeriodId: String, mode: String) {
        // Ingest new winning digit result for closed period
        val winningDigit = (0..9).random()
        repository.ingestServerPeriodResult(closedPeriodId, winningDigit, mode)

        if (_uiState.value.autoPredictEnabled) {
            generateMlPrediction()
        }
    }

    fun generateMlPrediction() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            delay(400L) // Visual matrix computation effect

            val mode = _uiState.value.selectedGameMode
            val algo = _uiState.value.selectedAlgorithm
            val prediction = repository.runMlPrediction(mode, algo)

            val currentHistory = _uiState.value.periodHistory
            val mlOutput = mlEngine.analyzeAndPredict(currentHistory, algo)

            _uiState.value = _uiState.value.copy(
                latestPrediction = prediction,
                mlOutputDetails = mlOutput,
                isAnalyzing = false
            )
        }
    }

    fun injectManualPeriod(periodId: String, number: Int) {
        viewModelScope.launch {
            val mode = _uiState.value.selectedGameMode
            repository.ingestServerPeriodResult(periodId, number, mode)
            generateMlPrediction()
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
