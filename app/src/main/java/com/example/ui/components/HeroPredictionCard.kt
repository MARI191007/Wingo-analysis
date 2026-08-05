package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PredictionResult
import com.example.data.repository.ServerPeriodInfo
import com.example.ml.WingoMlEngine
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveIndigoGlow
import com.example.ui.theme.ImmersiveIndigoLight
import com.example.ui.theme.ImmersiveIndigoPrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun HeroPredictionCard(
    serverInfo: ServerPeriodInfo?,
    prediction: PredictionResult?,
    isAnalyzing: Boolean,
    selectedAlgorithm: WingoMlEngine.AlgorithmType,
    onAlgorithmChange: (WingoMlEngine.AlgorithmType) -> Unit,
    onPredictClick: () -> Unit,
    onOpenSyncModal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaGlow"
    )

    Column(modifier = modifier.fillMaxWidth()) {

        // 1. Current Period Status Card (Immersive UI Style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT RUNNING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = serverInfo?.currentPeriodId ?: "2026080300458",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NEXT PREDICTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveIndigoLight,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = serverInfo?.nextPeriodId ?: "2026080300459",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ImmersiveIndigoLight,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LOCKING IN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val remaining = serverInfo?.secondsRemaining ?: 42
                        val minutes = remaining / 60
                        val seconds = remaining % 60
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LiveGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                val remaining = serverInfo?.secondsRemaining ?: 42
                val interval = serverInfo?.intervalSeconds ?: 60
                val progress = remaining.toFloat() / interval.toFloat()

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = ImmersiveIndigoPrimary,
                    trackColor = ImmersiveCardBorder
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    onClick = onOpenSyncModal,
                    shape = RoundedCornerShape(10.dp),
                    color = NeonGold.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_game_period_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = NeonGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ SYNC LIVE GAME PERIOD & INPUT REAL RESULTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonGold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. High Emphasis Prediction Core Card (Indigo Background)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            ImmersiveIndigoGlow.copy(alpha = alphaGlow),
                            LiveGreen.copy(alpha = alphaGlow * 0.7f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveIndigoPrimary)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Badge & Icon Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "CONFIDENCE: ${String.format(Locale.US, "%.1f", prediction?.bigSmallConfidence ?: 88.5f)}% (500 PERIODS)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { dropdownExpanded = true }
                                .testTag("algorithm_selector"),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Algo",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedAlgorithm.displayName.take(12),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(ImmersiveSurface)
                        ) {
                            WingoMlEngine.AlgorithmType.values().forEach { algo ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            algo.displayName,
                                            color = if (algo == selectedAlgorithm) ImmersiveIndigoLight else TextPrimary,
                                            fontWeight = if (algo == selectedAlgorithm) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onAlgorithmChange(algo)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAnalyzing) {
                    Column(
                        modifier = Modifier.height(140.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Analyzing Neural Matrix...",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (prediction == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = NeonGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "LIVE DATA COULD NOT BE FETCHED",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Live history from wingoanalyst.com could not be accessed. Analysis is stopped. Please click SYNC or check connection.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    val bs = prediction.predictedBigSmall
                    val targetPeriod = prediction.targetPeriodId

                    // Target Period Banner Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TARGET NEXT PERIOD: #$targetPeriod",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PROBABLE OUTCOME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = bs,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // White divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.25f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary and Secondary Numbers
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Primary Number (Solid White Card)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 8.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (prediction?.primaryNumber ?: 7).toString(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ImmersiveIndigoPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "PRIMARY (${String.format(Locale.US, "%.0f", prediction?.primaryProbability ?: 42f)}%)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Secondary Number (Glass Card)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (prediction?.secondaryNumber ?: 9).toString(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "SECONDARY (${String.format(Locale.US, "%.0f", prediction?.secondaryProbability ?: 35f)}%)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Run prediction action button
                Button(
                    onClick = onPredictClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("predict_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveBackground,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ImmersiveIndigoLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RE-CALCULATE PREDICTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
