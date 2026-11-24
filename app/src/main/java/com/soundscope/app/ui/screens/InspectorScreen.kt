package com.soundscope.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundscope.app.FoundDevice
import com.soundscope.app.ui.theme.SoundScopeTheme
import com.soundscope.app.ui.components.GlassCard
import com.soundscope.app.ui.theme.CyberTeal
import com.soundscope.app.ui.theme.ElectricBlue
import com.soundscope.app.ui.theme.ElectricPurple
import com.soundscope.app.ui.theme.EmeraldActive
import com.soundscope.app.ui.theme.NeonCyan
import com.soundscope.app.ui.theme.TextMuted
import com.soundscope.app.ui.theme.TextPrimary
import com.soundscope.app.ui.theme.TextSecondary

@Composable
fun InspectorScreen(
    selectedDevice: FoundDevice?,
    gattStatus: String,
    gattServices: List<String>,
    onInspectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SCREEN TITLE
        item {
            Column {
                Text(
                    text = "GATT SERVICE INSPECTOR",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "BLUETOOTH LE PROTOCOL EXPLORER",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }

        if (selectedDevice == null) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No Target Device Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Go to the RADAR tab and select any Bluetooth LE device to inspect its GATT services and characteristics.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            // TARGET DEVICE OVERVIEW CARD
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowEffect = true,
                    borderColor = NeonCyan
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedDevice.audioHint) Icons.Rounded.Headphones else Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedDevice.name.ifBlank { "Unidentified Device" },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = selectedDevice.address,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = selectedDevice.rssi?.let { "$it dBm" } ?: "—",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )
                        }

                        HorizontalDivider(color = Color(0xFF1E293B))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Protocol:", fontSize = 12.sp, color = TextMuted)
                            Text(
                                text = selectedDevice.source,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ElectricBlue
                            )
                        }

                        if (selectedDevice.paired) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Bonding State:", fontSize = 12.sp, color = TextMuted)
                                Text(
                                    text = "BONDED / PAIRED",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ElectricPurple
                                )
                            }
                        }

                        Button(
                            onClick = onInspectClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color(0xFF070B14)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ENUMERATE GATT SERVICES",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // CONNECTION STATUS CARD
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (gattStatus.contains("Connecting") || gattStatus.contains("Discovering")) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldActive,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "GATT CONNECTION STATUS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Text(
                                text = gattStatus.ifBlank { "Ready to inspect GATT services" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // DISCOVERED SERVICES HEADER
            item {
                Text(
                    text = "ENUMERATED GATT SERVICES (${gattServices.size})",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
            }

            // SERVICES LIST
            if (gattServices.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No GATT services enumerated yet. Tap ENUMERATE GATT SERVICES above.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            } else {
                items(gattServices) { service ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Build,
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = service,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B14)
@Composable
fun InspectorScreenPreview() {
    SoundScopeTheme {
        InspectorScreen(
            selectedDevice = FoundDevice(
                address = "7C:9A:1D:3B:4E:5F",
                name = "Sony WH-1000XM5",
                source = "Bluetooth LE",
                rssi = -52,
                paired = true,
                services = "00001844-0000-1000-8000-00805f9b34fb, 0000180f-0000-1000-8000-00805f9b34fb",
                manufacturerIds = "0x002D"
            ),
            gattStatus = "VCS found. Potential standard volume-control support.",
            gattServices = listOf(
                "Volume Control Service\n00001844-0000-1000-8000-00805f9b34fb",
                "Battery Service\n0000180f-0000-1000-8000-00805f9b34fb",
                "Device Information Service\n0000180a-0000-1000-8000-00805f9b34fb",
                "Audio Stream Control Service\n0000184e-0000-1000-8000-00805f9b34fb"
            ),
            onInspectClick = {}
        )
    }
}

