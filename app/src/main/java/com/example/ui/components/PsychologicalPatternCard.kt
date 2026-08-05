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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SecurityUpdateWarning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.MlPredictionOutput
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveIndigoGlow
import com.example.ui.theme.ImmersiveIndigoLight
import com.example.ui.theme.ImmersiveIndigoPrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PsychologicalPatternCard(
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

            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Psychology ML",
                        tint = NeonGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEURAL PATTERN & REVERSE TRAP ENGINE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeonGold.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "ML v4.2 Deep",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Forward Pattern vs Reverse Inversion Side-by-Side Comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Forward Pattern Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImmersiveBackground, RoundedCornerShape(16.dp))
                        .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ImmersiveIndigoLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FORWARD PATTERN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = mlOutput?.forwardPatternName ?: "ZIG-ZAG ALTERNATING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Match: ${mlOutput?.forwardPatternMatch?.toInt() ?: 85}%",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = mlOutput?.forwardPrediction ?: "BIG",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if ((mlOutput?.forwardPrediction ?: "BIG") == "BIG") ImmersiveIndigoLight else NeonOrange
                        )
                    }
                }

                // Reverse Pattern Card (Pattern Inversion)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ImmersiveBackground, RoundedCornerShape(16.dp))
                        .border(1.dp, NeonGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = NeonGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "REVERSE INVERSION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = mlOutput?.reversePatternName ?: "REVERSE-DRAGON FLIP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Score: ${mlOutput?.reverseInversionScore?.toInt() ?: 78}%",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = mlOutput?.reversePrediction ?: "SMALL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if ((mlOutput?.reversePrediction ?: "SMALL") == "BIG") ImmersiveIndigoLight else NeonOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Psychological Game Theory Dealer Trap Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ImmersiveBackground, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SecurityUpdateWarning,
                            contentDescription = null,
                            tint = if ((mlOutput?.dealerTrapScore ?: 35f) > 50f) NeonRed else LiveGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DEALER TRAP MODE: ${mlOutput?.dealerMode ?: "DRAGON BAIT TRAP"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "TRAP RISK: ${mlOutput?.dealerTrapScore?.toInt() ?: 35}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if ((mlOutput?.dealerTrapScore ?: 35f) > 50f) NeonRed else LiveGreen
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val trapProgress = ((mlOutput?.dealerTrapScore ?: 35f) / 100f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { trapProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (trapProgress > 0.5f) NeonRed else LiveGreen,
                    trackColor = ImmersiveCardBorder
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if ((mlOutput?.psychologicalAction ?: "").contains("COUNTER")) NeonGold.copy(alpha = 0.15f) else LiveGreen.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ RECOMMENDATION DIRECTIVE: ${mlOutput?.psychologicalAction ?: "COUNTER-ATTACK REVERSE"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if ((mlOutput?.psychologicalAction ?: "").contains("COUNTER")) NeonGold else LiveGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. AI Reasoning Explanation Text Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ImmersiveIndigoPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .border(1.dp, ImmersiveIndigoPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = mlOutput?.aiReasoningText ?: "Neural ensemble detected high dealer trap probability. Forward sequence favors BIG, but reverse inversion matrix strongly triggers SMALL.",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
