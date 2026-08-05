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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PeriodRecord
import com.example.data.model.PredictionResult
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveIndigoLight
import com.example.ui.theme.ImmersiveIndigoPrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.NeonGold
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
    onUpdatePeriodResult: ((periodId: String, digit: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val predictionMap = remember(predictions) {
        predictions.associateBy { it.targetPeriodId }
    }

    var editingPeriodId by remember { mutableStateOf<String?>(null) }
    var selectedDigitForEdit by remember { mutableStateOf(5) }

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
                                text = "↻ SYNC / FIX DATA",
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Live history data is syncing from Yaarwin / WinGo Servers...",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (onSyncClick != null) {
                        Button(
                            onClick = onSyncClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersiveIndigoPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FETCH & SYNC YAARWIN / WINGO DATA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
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
                        PeriodRowItem(
                            period = item,
                            prediction = prediction,
                            onEditClick = {
                                editingPeriodId = item.periodId
                                selectedDigitForEdit = item.number
                            }
                        )
                    }
                }
            }
        }
    }

    // Quick Edit Period Result Dialog
    if (editingPeriodId != null) {
        Dialog(onDismissRequest = { editingPeriodId = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SET EXACT WEBSITE RESULT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Period #${editingPeriodId?.takeLast(8)}",
                        fontSize = 11.sp,
                        color = NeonGold,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select actual winning digit on your website (0-9):",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (0..9).forEach { digit ->
                            val isSelected = selectedDigitForEdit == digit
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = if (isSelected) LiveGreen else ImmersiveBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) LiveGreen else ImmersiveCardBorder
                                ),
                                onClick = { selectedDigitForEdit = digit }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = digit.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ImmersiveBackground else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { editingPeriodId = null },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersiveCardBorder)
                        ) {
                            Text("CANCEL", fontSize = 10.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                editingPeriodId?.let { pId ->
                                    onUpdatePeriodResult?.invoke(pId, selectedDigitForEdit)
                                }
                                editingPeriodId = null
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LiveGreen)
                        ) {
                            Text("SAVE RESULT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ImmersiveBackground)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodRowItem(
    period: PeriodRecord,
    prediction: PredictionResult? = null,
    onEditClick: (() -> Unit)? = null
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
            // Header Row: Period ID, Verified Badge & AI Match
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Period #${period.periodId.takeLast(8)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (period.isRealVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LiveGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LiveGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LiveGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "WEBSITE DATA",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LiveGreen
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonOrange.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ESTIMATED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonOrange,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                text = if (isWin) "✓ AI WIN" else "✗ AI LOSS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isWin) LiveGreen else NeonRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (onEditClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Result",
                                tint = NeonGold,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
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
