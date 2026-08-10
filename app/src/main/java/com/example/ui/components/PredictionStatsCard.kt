package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PredictionResult
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveIndigoLight
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PredictionStatsCard(
    verifiedList: List<PredictionResult>,
    winCount: Int,
    totalCount: Int,
    winRate: Float,
    onResetWinRate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Calculate Breakdown: Jackpot Wins (exact digit match), Standard Wins, Losses
    val jackpotWins = verifiedList.count { item ->
        item.actualNumber != null &&
                (item.primaryNumber == item.actualNumber || item.secondaryNumber == item.actualNumber)
    }
    val standardWins = (winCount - jackpotWins).coerceAtLeast(0)
    val losses = (totalCount - winCount).coerceAtLeast(0)

    val jackpotPct = if (totalCount > 0) (jackpotWins.toFloat() / totalCount) * 100f else 0f
    val standardPct = if (totalCount > 0) (standardWins.toFloat() / totalCount) * 100f else 0f
    val lossPct = if (totalCount > 0) (losses.toFloat() / totalCount) * 100f else 0f

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
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Stats",
                        tint = ImmersiveIndigoLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ML VERIFIED ACCURACY STATS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LiveGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, LiveGreen.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", winRate)}% WIN RATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LiveGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scoreboard Summary Cards (Grid of 4 Metrics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TOTAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(totalCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("WINS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(standardWins.toString(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = LiveGreen)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = NeonGold, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("JACKPOT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonGold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(jackpotWins.toString(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonGold)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LOSSES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(losses.toString(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonRed)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // VISUAL ACCURACY DISTRIBUTION CHART
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(14.dp))
                    .background(ImmersiveBackground, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "ACCURACY DISTRIBUTION CHART",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Multi-Segment Proportion Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TextMuted.copy(alpha = 0.2f))
                ) {
                    if (totalCount > 0) {
                        if (jackpotWins > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(jackpotWins.toFloat())
                                    .height(10.dp)
                                    .background(NeonGold)
                            )
                        }
                        if (standardWins > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(standardWins.toFloat())
                                    .height(10.dp)
                                    .background(LiveGreen)
                            )
                        }
                        if (losses > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(losses.toFloat())
                                    .height(10.dp)
                                    .background(NeonRed)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chart Legend / Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(NeonGold, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Jackpot: ${jackpotWins} (${String.format(Locale.US, "%.0f", jackpotPct)}%)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(LiveGreen, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Wins: ${standardWins} (${String.format(Locale.US, "%.0f", standardPct)}%)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = LiveGreen
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(NeonRed, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Losses: ${losses} (${String.format(Locale.US, "%.0f", lossPct)}%)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RESET WIN RATE BUTTON
            OutlinedButton(
                onClick = onResetWinRate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Win Rate",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RESET WIN RATE DATA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of Recent Verified Predictions
            Text(
                text = "PREDICTION VERIFICATION LOG",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (verifiedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Predictions will verify automatically when period closes...", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .testTag("verified_predictions_list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(verifiedList) { item ->
                        val isNumberWin = item.actualNumber != null &&
                                (item.primaryNumber == item.actualNumber || item.secondaryNumber == item.actualNumber)
                        val isBigSmallWin = (item.actualBigSmall != null && item.predictedBigSmall == item.actualBigSmall) || (item.isWin == true && !isNumberWin)

                        val statusText = when {
                            isNumberWin -> "JACKPOT WIN"
                            isBigSmallWin -> "WIN"
                            else -> "LOSE"
                        }

                        val badgeBg = when {
                            isNumberWin -> NeonGold.copy(alpha = 0.2f)
                            isBigSmallWin -> LiveGreen.copy(alpha = 0.15f)
                            else -> NeonRed.copy(alpha = 0.15f)
                        }

                        val badgeColor = when {
                            isNumberWin -> NeonGold
                            isBigSmallWin -> LiveGreen
                            else -> NeonRed
                        }

                        val statusIcon = when {
                            isNumberWin -> Icons.Default.Star
                            isBigSmallWin -> Icons.Default.CheckCircle
                            else -> Icons.Default.ThumbDown
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ImmersiveBackground
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Period: ${item.targetPeriodId}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Pred: ${item.predictedBigSmall} [${item.primaryNumber}, ${item.secondaryNumber}] | Actual: ${item.actualBigSmall ?: "-"} (${item.actualNumber ?: "-"})",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = statusIcon,
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = statusText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = badgeColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
