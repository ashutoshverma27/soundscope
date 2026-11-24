package com.soundscope.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundscope.app.ui.theme.SoundScopeTheme
import com.soundscope.app.ui.components.AudioWaveformCanvas
import com.soundscope.app.ui.components.DecibelRingMeter
import com.soundscope.app.ui.components.GlassCard
import com.soundscope.app.ui.components.StatWidget
import com.soundscope.app.ui.theme.AmberGlow
import com.soundscope.app.ui.theme.CrimsonPeak
import com.soundscope.app.ui.theme.DeepSpaceCard
import com.soundscope.app.ui.theme.EmeraldActive
import com.soundscope.app.ui.theme.NeonCyan
import com.soundscope.app.ui.theme.TextMuted
import com.soundscope.app.ui.theme.TextPrimary
import com.soundscope.app.ui.theme.TextSecondary

@Composable
fun AudioScreen(
    micRunning: Boolean,
    currentDbfs: Double?,
    peakDbfs: Double?,
    micStatus: String,
    dbfsHistory: List<Double>,
    onRequestMic: () -> Unit,
    onStopMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val micAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micAlpha"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SCREEN HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ROOM NOISE MONITOR",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = micStatus.ifBlank { "ACOUSTIC LEVEL MONITOR" },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                Surface(
                    color = if (micRunning) EmeraldActive.copy(alpha = 0.15f) else DeepSpaceCard,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (micRunning) EmeraldActive else TextMuted)
                                .alpha(if (micRunning) micAlpha else 1.0f)
                        )
                        Text(
                            text = if (micRunning) "RECORDING" else "OFFLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (micRunning) EmeraldActive else TextMuted
                        )
                    }
                }
            }
        }

        // DECIBEL RING GAUGE CARD
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowEffect = micRunning,
                borderColor = if (micRunning) NeonCyan else Color(0x2B38BDF8)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "LIVE ACOUSTIC LEVEL",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Text(
                            text = "PCM relative dBFS",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }

                    DecibelRingMeter(
                        currentDbfs = currentDbfs,
                        peakDbfs = peakDbfs,
                        active = micRunning,
                        modifier = Modifier.size(220.dp)
                    )

                    // Controls Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onRequestMic,
                            enabled = !micRunning,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color(0xFF070B14)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "START MONITOR",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onStopMic,
                            enabled = micRunning,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "STOP",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // STATS CARDS ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatWidget(
                    label = "CURRENT",
                    value = currentDbfs?.let { "%.1f".format(it) } ?: "—",
                    icon = Icons.Rounded.VolumeUp,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                StatWidget(
                    label = "PEAK LEVEL",
                    value = peakDbfs?.let { "%.1f".format(it) } ?: "—",
                    icon = Icons.Rounded.GraphicEq,
                    accentColor = CrimsonPeak,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // WAVEFORM CANVAS CARD
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "REAL-TIME ACOUSTIC WAVEFORM",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.2.sp
                    )

                    AudioWaveformCanvas(
                        history = dbfsHistory,
                        active = micRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            }
        }

        // NOTICE FOOTER
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = AmberGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Acoustic Intelligence Disclosure",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Readings are relative full-scale decibels (dBFS) computed directly from PCM microphone input buffers. They reflect relative ambient sound levels and are not calibrated to absolute SPL (dB A).",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B14)
@Composable
fun AudioScreenPreview() {
    SoundScopeTheme {
        AudioScreen(
            micRunning = true,
            currentDbfs = -34.8,
            peakDbfs = -18.2,
            micStatus = "Microphone active.",
            dbfsHistory = listOf(
                -70.0, -65.0, -58.0, -50.0, -42.0, -38.0, -32.0, -28.0, -22.0, -18.2,
                -25.0, -30.0, -36.0, -40.0, -48.0, -52.0, -44.0, -34.8, -30.0, -26.0
            ),
            onRequestMic = {},
            onStopMic = {}
        )
    }
}

