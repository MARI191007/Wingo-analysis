package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PeriodRecord
import com.example.data.model.PredictionResult
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveIndigoLight
import com.example.ui.theme.ImmersiveIndigoPrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PeriodHistoryTable(
    periods: List<PeriodRecord>,
    predictions: List<PredictionResult> = emptyList(),
    onSyncClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val predictionMap = remember(predictions) {
        predictions.associateBy { it.targetPeriodId }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = ImmersiveIndigoLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PERIOD HISTORY (500 RESULTS)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onSyncClick != null) {
                        Surface(
                            onClick = onSyncClick,
                            shape = RoundedCornerShape(10.dp),
                            color = LiveGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LiveGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "↻ SYNC 500",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LiveGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ImmersiveBackground
                    ) {
                        Text(
                            text = "${periods.size} / 500",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Quick-Node Ribbon (Immersive UI style horizontally scrollable history preview)
            if (periods.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ImmersiveBackground, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    periods.take(6).reversed().forEach { item ->
                        val isBig = item.bigSmall == "BIG"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = if (isBig) ImmersiveIndigoPrimary.copy(alpha = 0.2f) else ImmersiveCardBorder,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isBig) ImmersiveIndigoLight else TextMuted
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.number.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBig) ImmersiveIndigoLight else TextPrimary
                                    )
                                }
                            }
                            Text(
                                text = if (isBig) "B" else "S",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isBig) ImmersiveIndigoLight else TextMuted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (periods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Synchronizing with Wingo server...", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .testTag("period_history_list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(periods, key = { it.periodId }) { item ->
                        val prediction = predictionMap[item.periodId]
                        PeriodRowItem(period = item, prediction = prediction)
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodRowItem(
    period: PeriodRecord,
    prediction: PredictionResult? = null
) {
    val isBig = period.bigSmall == "BIG"
    val bsBg = if (isBig) ImmersiveIndigoPrimary.copy(alpha = 0.2f) else NeonOrange.copy(alpha = 0.15f)
    val bsTextColor = if (isBig) ImmersiveIndigoLight else NeonOrange

    val colorHex = when (period.color) {
        "RED" -> NeonRed
        "VIOLET" -> NeonViolet
        else -> LiveGreen
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ImmersiveBackground.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Header Row: Period ID & AI Match Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Period #${period.periodId.takeLast(8)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (prediction != null) {
                    val isWin = prediction.isWin == true
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isWin) LiveGreen.copy(alpha = 0.15f) else NeonRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isWin) LiveGreen.copy(alpha = 0.3f) else NeonRed.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = if (isWin) "✓ AI MATCH WIN" else "✗ AI LOSS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isWin) LiveGreen else NeonRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "ORIGINAL RESULT",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Row: Original Result vs AI Prediction Comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Actual Server Result
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Actual: ", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = colorHex.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorHex)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = period.number.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorHex
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = bsBg
                    ) {
                        Text(
                            text = period.bigSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = bsTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // AI Prediction Result
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Predicted: ", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    if (prediction != null) {
                        val predIsBig = prediction.predictedBigSmall == "BIG"
                        val predBg = if (predIsBig) ImmersiveIndigoPrimary.copy(alpha = 0.2f) else NeonOrange.copy(alpha = 0.15f)
                        val predColor = if (predIsBig) ImmersiveIndigoLight else NeonOrange

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = predBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, predColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${prediction.predictedBigSmall} [${prediction.primaryNumber}, ${prediction.secondaryNumber}]",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = predColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text("No Log", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Normal)
                    }
                }
            }
        }
    }
}
