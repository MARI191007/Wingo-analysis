package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AnalyticsHeatmapCard
import com.example.ui.components.BottomNav
import com.example.ui.components.EngineSettingsCard
import com.example.ui.components.HeaderBar
import com.example.ui.components.HeroPredictionCard
import com.example.ui.components.NavigationTab
import com.example.ui.components.PeriodHistoryTable
import com.example.ui.components.PredictionStatsCard
import com.example.ui.components.PsychologicalPatternCard
import com.example.ui.components.SyncRealPeriodDialog
import com.example.ui.components.YaarwinWebCard
import com.example.ui.theme.ImmersiveBackground

@Composable
fun MainPredictorScreen(
    viewModel: WingoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(NavigationTab.PREDICT) }

    if (uiState.isSyncModalOpen) {
        SyncRealPeriodDialog(
            currentPeriodId = uiState.serverInfo?.currentPeriodId ?: "",
            onDismiss = { viewModel.setSyncModalOpen(false) },
            onSyncBasePeriod = { periodId ->
                viewModel.updateCustomBasePeriod(periodId)
                viewModel.setSyncModalOpen(false)
            },
            onSubmitRealResult = { periodId, winningDigit ->
                viewModel.injectManualPeriod(periodId, winningDigit)
            },
            onBatchImportText = { text ->
                viewModel.batchImportWebsiteHistory(text)
                viewModel.setSyncModalOpen(false)
            }
        )
    }

    Scaffold(
        containerColor = ImmersiveBackground,
        bottomBar = {
            BottomNav(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ImmersiveBackground)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Sticky Top Header with Game Mode Tabs
            HeaderBar(
                selectedGameMode = uiState.selectedGameMode,
                onGameModeSelected = { viewModel.selectGameMode(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Content view switching depending on BottomNav tab selection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabSwitch"
                ) { target ->
                    when (target) {
                        NavigationTab.PREDICT -> {
                            Column {
                                HeroPredictionCard(
                                    serverInfo = uiState.serverInfo,
                                    prediction = uiState.latestPrediction,
                                    isAnalyzing = uiState.isAnalyzing,
                                    selectedAlgorithm = uiState.selectedAlgorithm,
                                    onAlgorithmChange = { viewModel.selectAlgorithm(it) },
                                    onPredictClick = { viewModel.generateMlPrediction() },
                                    onOpenSyncModal = { viewModel.setSyncModalOpen(true) }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                PsychologicalPatternCard(mlOutput = uiState.mlOutputDetails)

                                Spacer(modifier = Modifier.height(16.dp))

                                PeriodHistoryTable(
                                    periods = uiState.periodHistory,
                                    predictions = uiState.verifiedPredictions,
                                    onSyncClick = { viewModel.sync500OnlinePeriods() },
                                    onUpdatePeriodResult = { periodId, digit ->
                                        viewModel.injectManualPeriod(periodId, digit)
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                AnalyticsHeatmapCard(mlOutput = uiState.mlOutputDetails)

                                Spacer(modifier = Modifier.height(16.dp))

                                // Embedded Yaarwin Live Portal directly at the bottom
                                YaarwinWebCard(
                                    currentGameMode = uiState.selectedGameMode,
                                    onHistoryExtracted = { records ->
                                        viewModel.ingestYaarwinLiveHistory(records)
                                    }
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        NavigationTab.YAARWIN -> {
                            Column {
                                YaarwinWebCard(
                                    currentGameMode = uiState.selectedGameMode,
                                    onHistoryExtracted = { records ->
                                        viewModel.ingestYaarwinLiveHistory(records)
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                PeriodHistoryTable(
                                    periods = uiState.periodHistory,
                                    predictions = uiState.verifiedPredictions,
                                    onSyncClick = { viewModel.sync500OnlinePeriods() },
                                    onUpdatePeriodResult = { periodId, digit ->
                                        viewModel.injectManualPeriod(periodId, digit)
                                    }
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        NavigationTab.HISTORY -> {
                            Column {
                                PeriodHistoryTable(
                                    periods = uiState.periodHistory,
                                    predictions = uiState.verifiedPredictions,
                                    onSyncClick = { viewModel.sync500OnlinePeriods() },
                                    onUpdatePeriodResult = { periodId, digit ->
                                        viewModel.injectManualPeriod(periodId, digit)
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        NavigationTab.ANALYTICS -> {
                            Column {
                                PsychologicalPatternCard(mlOutput = uiState.mlOutputDetails)

                                Spacer(modifier = Modifier.height(16.dp))

                                AnalyticsHeatmapCard(mlOutput = uiState.mlOutputDetails)

                                Spacer(modifier = Modifier.height(16.dp))

                                PredictionStatsCard(
                                    verifiedList = uiState.verifiedPredictions,
                                    winCount = uiState.winCount,
                                    totalCount = uiState.totalVerifiedCount,
                                    winRate = uiState.winRatePercentage
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        NavigationTab.ENGINE -> {
                            Column {
                                EngineSettingsCard(
                                    autoPredictEnabled = uiState.autoPredictEnabled,
                                    onAutoPredictToggle = { viewModel.toggleAutoPredict(it) },
                                    serverLatencyMs = uiState.serverLatencyMs,
                                    onInjectDigit = { digit ->
                                        uiState.serverInfo?.let { info ->
                                            viewModel.injectManualPeriod(info.currentPeriodId, digit)
                                        }
                                    },
                                    onResetData = { viewModel.resetData() },
                                    onOpenSyncModal = { viewModel.setSyncModalOpen(true) }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                PredictionStatsCard(
                                    verifiedList = uiState.verifiedPredictions,
                                    winCount = uiState.winCount,
                                    totalCount = uiState.totalVerifiedCount,
                                    winRate = uiState.winRatePercentage
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
