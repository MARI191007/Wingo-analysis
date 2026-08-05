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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveIndigoLight
import com.example.ui.theme.ImmersiveIndigoPrimary
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HeaderBar(
    selectedGameMode: String,
    onGameModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liveDot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Top Row: App Title & Live Server Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WINGO AI PROCESSOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ImmersiveIndigoLight,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Neural Engine V4.2",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            // Live Server Pill Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LiveGreen.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, LiveGreen.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(LiveGreen.copy(alpha = dotAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WINGOANALYST.COM LIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LiveGreen,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Game Mode Tab Selector (1Min, 3Min, 5Min, 10Min)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImmersiveBackground, RoundedCornerShape(16.dp))
                .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("1Min", "3Min", "5Min", "10Min").forEach { mode ->
                val isSelected = mode == selectedGameMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ImmersiveIndigoPrimary else ImmersiveBackground)
                        .clickable { onGameModeSelected(mode) }
                        .testTag("mode_tab_$mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextMuted
                    )
                }
            }
        }
    }
}
