package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.ml.MlPredictionOutput
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveIndigoLight
import com.example.ui.theme.ImmersiveIndigoPrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AnalyticsHeatmapCard(
    mlOutput: MlPredictionOutput?,
    modifier: Modifier = Modifier
) {
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
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = ImmersiveIndigoLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "50-PERIOD MARKOV HEATMAP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ImmersiveBackground
                ) {
                    Text(
                        text = "ML Matrix",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val heatmap = mlOutput?.heatmapInfo

            // 1. Distribution Stats Bar (Big vs Small count)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BIG Ratio: ${heatmap?.bigCount50 ?: 26}/50",
                    fontSize = 11.sp,
                    color = ImmersiveIndigoLight,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SMALL Ratio: ${heatmap?.smallCount50 ?: 24}/50",
                    fontSize = 11.sp,
                    color = NeonOrange,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val bigRatio = (heatmap?.bigCount50 ?: 26).toFloat() / 50f
            LinearProgressIndicator(
                progress = { bigRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = ImmersiveIndigoPrimary,
                trackColor = NeonOrange.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Hot & Cold Numbers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hot Numbers Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImmersiveBackground, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Hot",
                            tint = NeonRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HOT DIGITS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val hots = heatmap?.hotNumbers ?: listOf(7, 3, 8)
                        hots.forEach { num ->
                            Surface(
                                modifier = Modifier.size(26.dp),
                                shape = CircleShape,
                                color = NeonRed.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(num.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonRed)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Streak Reversal Index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImmersiveBackground, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text("CURRENT STREAK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = heatmap?.currentStreak ?: "BIG x3",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ImmersiveIndigoLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Reversal Prob: ${String.format(Locale.US, "%.0f", heatmap?.streakReversalIndex ?: 48f)}%",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Digit Probability Distribution Histogram
            Text(
                text = "0-9 DIGIT PROBABILITY DISTRIBUTION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val digitProbs = mlOutput?.digitProbabilities ?: (0..9).map { it to (if (it == 7) 22f else 8.6f) }
            val mapProbs = digitProbs.toMap()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(ImmersiveBackground, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                (0..9).forEach { digit ->
                    val prob = mapProbs[digit] ?: 10f
                    val normalizedHeight = (prob / 30f).coerceIn(0.1f, 1.0f)
                    val isTop = digit == mlOutput?.primaryNumber || digit == mlOutput?.secondaryNumber

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${prob.toInt()}%",
                            fontSize = 8.sp,
                            color = if (isTop) NeonGold else TextMuted,
                            fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(normalizedHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (isTop) ImmersiveIndigoLight else ImmersiveIndigoPrimary.copy(alpha = 0.4f)
                                )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = digit.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTop) TextPrimary else TextMuted
                        )
                    }
                }
            }
        }
    }
}
