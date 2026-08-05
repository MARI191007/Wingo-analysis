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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ImmersiveBackground
import com.example.ui.theme.ImmersiveCardBorder
import com.example.ui.theme.ImmersiveIndigoLight
import com.example.ui.theme.ImmersiveIndigoPrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.NeonGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SyncRealPeriodDialog(
    currentPeriodId: String,
    onDismiss: () -> Unit,
    onSyncBasePeriod: (String) -> Unit,
    onSubmitRealResult: (periodId: String, winningDigit: Int) -> Unit
) {
    var periodInput by remember { mutableStateOf(currentPeriodId) }
    var selectedDigit by remember { mutableStateOf(5) }
    var statusMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ImmersiveCardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = NeonGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYNC REAL GAME DATA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "If your original game's Period ID or winning results don't match, sync directly with WingoAnalyst or enter your live game's Period ID below.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section 0: One-Tap WingoAnalyst Sync
                Button(
                    onClick = {
                        onSyncBasePeriod("WINGOANALYST_AUTO_SYNC")
                        statusMessage = "Syncing 99 live drawn results from wingoanalyst.com..."
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGold)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = ImmersiveBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SYNC 99 RESULTS FROM WINGOANALYST.COM", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = ImmersiveBackground)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Set Live Period ID
                Text(
                    text = "1. SET CURRENT LIVE PERIOD ID",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveIndigoLight,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = periodInput,
                    onValueChange = { periodInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("period_id_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ImmersiveIndigoLight,
                        unfocusedBorderColor = ImmersiveCardBorder,
                        focusedContainerColor = ImmersiveBackground,
                        unfocusedContainerColor = ImmersiveBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    placeholder = { Text("e.g. 202608031000492", fontSize = 12.sp, color = TextMuted) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (periodInput.isNotBlank()) {
                            onSyncBasePeriod(periodInput)
                            statusMessage = "Synced base Period ID to #${periodInput}!"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveIndigoPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LOCK PERIOD SEQUENCER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Input Actual Winning Number for Period
                Text(
                    text = "2. RECORD ACTUAL GAME RESULT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveIndigoLight,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    text = "Select actual digit (0-9) that won in period #${periodInput.takeLast(6)}:",
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (0..9).forEach { digit ->
                        val isSelected = selectedDigit == digit
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = if (isSelected) LiveGreen else ImmersiveBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) LiveGreen else ImmersiveCardBorder
                            ),
                            onClick = { selectedDigit = digit }
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

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (periodInput.isNotBlank()) {
                            onSubmitRealResult(periodInput, selectedDigit)
                            statusMessage = "Logged Period #${periodInput} result: $selectedDigit!"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LiveGreen)
                ) {
                    Text("ADD REAL RESULT TO HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ImmersiveBackground)
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGold
                    )
                }
            }
        }
    }
}
