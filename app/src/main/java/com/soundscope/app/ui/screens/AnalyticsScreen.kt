package com.soundscope.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundscope.app.FoundDevice
import com.soundscope.app.ui.theme.SoundScopeTheme
import com.soundscope.app.ui.components.GlassCard
import com.soundscope.app.ui.components.StatWidget
import com.soundscope.app.ui.theme.CyberTeal
import com.soundscope.app.ui.theme.ElectricBlue
import com.soundscope.app.ui.theme.ElectricPurple
import com.soundscope.app.ui.theme.NeonCyan
import com.soundscope.app.ui.theme.SignalExcellent
import com.soundscope.app.ui.theme.SignalFair
import com.soundscope.app.ui.theme.SignalGood
import com.soundscope.app.ui.theme.SignalWeak
import com.soundscope.app.ui.theme.TextMuted
import com.soundscope.app.ui.theme.TextPrimary
import com.soundscope.app.ui.theme.TextSecondary

@Composable
fun AnalyticsScreen(
    devices: List<FoundDevice>,
    onExportReport: () -> Unit,
    onClearResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bleCount = devices.count { it.source.contains("BLE", ignoreCase = true) }
    val classicCount = devices.count { it.source.contains("Classic", ignoreCase = true) }
    val audioCount = devices.count { it.audioHint }

    val excellentCount = devices.count { (it.rssi ?: -100) > -60 }
    val goodCount = devices.count { (it.rssi ?: -100) in -75..-60 }
    val fairCount = devices.count { (it.rssi ?: -100) in -88..-76 }
    val weakCount = devices.count { (it.rssi ?: -100) < -88 }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SCREEN TITLE
        item {
            Column {
                Text(
                    text = "INTELLIGENCE HUB",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "RF ENVIRONMENT SUMMARY & ANALYTICS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }

        // STATS SUMMARY ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatWidget(
                    label = "DISCOVERED",
                    value = devices.size.toString(),
                    icon = Icons.Rounded.Analytics,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                StatWidget(
                    label = "AUDIO DEVS",
                    value = audioCount.toString(),
                    icon = Icons.Rounded.Headphones,
                    accentColor = CyberTeal,
                    modifier = Modifier.weight(1f)
                )

                StatWidget(
                    label = "BLE RATIO",
                    value = if (devices.isEmpty()) "0%" else "${(bleCount * 100) / devices.size}%",
                    icon = Icons.Rounded.Bluetooth,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // PROTOCOL DISTRIBUTION CARD
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PROTOCOL DISTRIBUTION",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.2.sp
                    )

                    // Multi-Segment Proportion Bar
                    if (devices.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            if (bleCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(bleCount.toFloat())
                                        .fillMaxSize()
                                        .background(NeonCyan)
                                )
                            }
                            if (classicCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(classicCount.toFloat())
                                        .fillMaxSize()
                                        .background(ElectricPurple)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(NeonCyan)
                            )
                            Text(
                                text = "BLE Advertisements ($bleCount)",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(ElectricPurple)
                            )
                            Text(
                                text = "Classic Bluetooth ($classicCount)",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // SIGNAL STRENGTH BREAKDOWN CARD
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SIGNAL STRENGTH DISTRIBUTION",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.2.sp
                    )

                    SignalRow("Excellent (> -60 dBm)", excellentCount, SignalExcellent, devices.size)
                    SignalRow("Good (-60 to -75 dBm)", goodCount, SignalGood, devices.size)
                    SignalRow("Fair (-76 to -88 dBm)", fairCount, SignalFair, devices.size)
                    SignalRow("Weak (< -88 dBm)", weakCount, SignalWeak, devices.size)
                }
            }
        }

        // ACTION BUTTONS CARD
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onExportReport,
                        enabled = devices.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF070B14)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "EXPORT INTELLIGENCE REPORT",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onClearResults,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "CLEAR SCAN RESULTS",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // DISCLAIMER
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "SoundScope non-intrusively discovers and inspects Bluetooth Classic and BLE advertisements. It strictly complies with Android security and privacy protocols. Microphone dBFS readings are relative PCM audio levels.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B14)
@Composable
fun AnalyticsScreenPreview() {
    SoundScopeTheme {
        AnalyticsScreen(
            devices = listOf(
                FoundDevice(
                    address = "AA:BB:CC:DD:EE:01",
                    name = "Sony WH-1000XM5",
                    source = "Bluetooth LE",
                    rssi = -52,
                    paired = true,
                    services = "00001844-0000-1000-8000-00805f9b34fb",
                    manufacturerIds = "0x002D"
                ),
                FoundDevice(
                    address = "AA:BB:CC:DD:EE:02",
                    name = "JBL Flip 6",
                    source = "Bluetooth LE",
                    rssi = -71,
                    paired = false,
                    services = "0000184e-0000-1000-8000-00805f9b34fb",
                    manufacturerIds = "0x0057"
                ),
                FoundDevice(
                    address = "AA:BB:CC:DD:EE:03",
                    name = "Apple Watch Ultra",
                    source = "Bluetooth LE",
                    rssi = -64,
                    paired = true,
                    services = "0000180a-0000-1000-8000-00805f9b34fb",
                    manufacturerIds = "0x004C"
                ),
                FoundDevice(
                    address = "AA:BB:CC:DD:EE:04",
                    name = "Old Car Audio",
                    source = "Bluetooth Classic",
                    rssi = -92,
                    paired = false,
                    services = "",
                    manufacturerIds = ""
                )
            ),
            onExportReport = {},
            onClearResults = {}
        )
    }
}


@Composable
private fun SignalRow(label: String, count: Int, color: Color, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            Text(
                text = "$count devices",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1E293B))
        ) {
            if (fraction > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}
