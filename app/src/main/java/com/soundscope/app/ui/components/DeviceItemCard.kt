package com.soundscope.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Phonelink
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundscope.app.FoundDevice
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceItemCard(
    device: FoundDevice,
    isSelected: Boolean,
    onInspectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rssi = device.rssi
    val signalColor = when {
        rssi == null -> TextMuted
        rssi > -60 -> SignalExcellent
        rssi > -75 -> SignalGood
        rssi > -88 -> SignalFair
        else -> SignalWeak
    }

    val icon = when {
        device.audioHint -> Icons.Rounded.Headphones
        device.name.lowercase().contains("watch") -> Icons.Rounded.Watch
        device.name.lowercase().contains("mac") || device.name.lowercase().contains("pc") -> Icons.Rounded.Computer
        device.name.lowercase().contains("phone") -> Icons.Rounded.Phonelink
        device.source.contains("BLE") -> Icons.Rounded.Router
        else -> Icons.Rounded.Bluetooth
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        glowEffect = isSelected,
        borderColor = if (isSelected) NeonCyan else Color(0x2B38BDF8),
        onClick = onInspectClick
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Top Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Device Avatar / Category Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (device.audioHint) NeonCyan.copy(alpha = 0.15f)
                            else ElectricBlue.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Device Category",
                        tint = if (device.audioHint) NeonCyan else ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Device Name & Source Protocol
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = device.name.ifBlank { "Unidentified Signal" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${device.source} • ${device.address}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                // Signal Strength Meter (RSSI)
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = rssi?.let { "$it dBm" } ?: "—",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = signalColor
                    )
                    Spacer(Modifier.height(3.dp))
                    SignalBars(rssi = rssi, signalColor = signalColor)
                }
            }

            // Badges Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (device.paired) {
                    BadgePill(text = "PAIRED", color = ElectricPurple)
                }
                if (device.audioHint) {
                    BadgePill(text = "AUDIO CAPABLE", color = NeonCyan)
                }
                if (device.advertisesVcs) {
                    BadgePill(text = "VCS ADVERTISED", color = CyberTeal)
                }
            }

            // Detailed information drawer
            AnimatedVisibility(
                visible = isSelected || device.services.isNotBlank() || device.manufacturerIds.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF1E293B))

                    if (device.services.isNotBlank()) {
                        Text(
                            text = "Services: ${device.services}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }

                    if (device.manufacturerIds.isNotBlank()) {
                        Text(
                            text = "Manufacturer Data: ${device.manufacturerIds}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }

                    if (device.source.contains("Bluetooth LE")) {
                        OutlinedButton(
                            onClick = onInspectClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonCyan
                            )
                        ) {
                            Text(
                                "Inspect GATT Services",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            color = color,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SignalBars(rssi: Int?, signalColor: Color) {
    val barCount = 4
    val activeBars = when {
        rssi == null -> 0
        rssi > -60 -> 4
        rssi > -72 -> 3
        rssi > -84 -> 2
        else -> 1
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..barCount) {
            val isActive = i <= activeBars
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((i * 3 + 2).dp)
                    .clip(CircleShape)
                    .background(if (isActive) signalColor else Color(0xFF334155))
            )
        }
    }
}
