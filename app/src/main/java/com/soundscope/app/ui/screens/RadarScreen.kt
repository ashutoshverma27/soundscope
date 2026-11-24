package com.soundscope.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundscope.app.FoundDevice
import com.soundscope.app.ui.theme.SoundScopeTheme
import com.soundscope.app.ui.components.DeviceItemCard
import com.soundscope.app.ui.components.GlassCard
import com.soundscope.app.ui.components.RadarCanvas
import com.soundscope.app.ui.components.StatWidget
import com.soundscope.app.ui.theme.CyberTeal
import com.soundscope.app.ui.theme.DeepSpaceCard
import com.soundscope.app.ui.theme.ElectricBlue
import com.soundscope.app.ui.theme.ElectricPurple
import com.soundscope.app.ui.theme.EmeraldActive
import com.soundscope.app.ui.theme.NeonCyan
import com.soundscope.app.ui.theme.TextMuted
import com.soundscope.app.ui.theme.TextPrimary
import com.soundscope.app.ui.theme.TextSecondary

@Composable
fun RadarScreen(
    scanning: Boolean,
    statusText: String,
    devices: List<FoundDevice>,
    filter: String,
    onFilterChanged: (String) -> Unit,
    selectedDevice: FoundDevice?,
    onDeviceSelected: (FoundDevice) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredDevices = remember(devices, filter, searchQuery) {
        devices.filter { device ->
            val matchesFilter = when (filter) {
                "Audio" -> device.audioHint
                "Paired" -> device.paired
                "BLE" -> device.source.contains("BLE")
                "Classic" -> device.source.contains("Classic")
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    device.name.contains(searchQuery, ignoreCase = true) ||
                    device.address.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER BAR
        item {
            HeaderBar(scanning = scanning, statusText = statusText)
        }

        // RADAR CANVAS
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                glowEffect = scanning,
                borderColor = if (scanning) NeonCyan else Color(0x2B38BDF8)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                imageVector = Icons.Rounded.Radar,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "LIVE SIGNAL RADAR",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Text(
                            text = "${devices.size} NODES",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }

                    RadarCanvas(
                        scanning = scanning,
                        devices = filteredDevices,
                        selectedDevice = selectedDevice,
                        onDeviceSelected = onDeviceSelected
                    )

                    // Controls Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onStartScan,
                            enabled = !scanning,
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
                                text = "START SCAN",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onStopScan,
                            enabled = scanning,
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

        // STATS SUMMARY ROW
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatWidget(
                    label = "TOTAL",
                    value = devices.size.toString(),
                    icon = Icons.Rounded.Bluetooth,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                StatWidget(
                    label = "AUDIO",
                    value = devices.count { it.audioHint }.toString(),
                    icon = Icons.Rounded.Headphones,
                    accentColor = CyberTeal,
                    modifier = Modifier.weight(1f)
                )

                StatWidget(
                    label = "PAIRED",
                    value = devices.count { it.paired }.toString(),
                    icon = Icons.Rounded.Bluetooth,
                    accentColor = ElectricPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // SEARCH & FILTER BAR
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search device name or address...",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DeepSpaceCard,
                        unfocusedContainerColor = DeepSpaceCard,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    val filterOptions = listOf("All", "Audio", "Paired", "BLE", "Classic")
                    items(filterOptions) { opt ->
                        val isSelected = filter == opt
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterChanged(opt) },
                            label = {
                                Text(
                                    text = opt,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan,
                                containerColor = DeepSpaceCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFF1E293B),
                                selectedBorderColor = NeonCyan
                            )
                        )
                    }
                }
            }
        }

        // DISCOVERED DEVICES LIST
        if (filteredDevices.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Radar,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (devices.isEmpty()) "No devices discovered yet" else "No matching signals",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (devices.isEmpty()) "Tap START SCAN to discover nearby Bluetooth & BLE devices."
                            else "Try adjusting your search query or filter tags.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredDevices, key = { it.address }) { device ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DeviceItemCard(
                        device = device,
                        isSelected = selectedDevice?.address == device.address,
                        onInspectClick = { onDeviceSelected(device) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(scanning: Boolean, statusText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = "SOUNDSCOPE",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                color = TextPrimary
            )
            Text(
                text = statusText.ifBlank { "RF INTELLIGENCE HUB" },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }

        Surface(
            color = if (scanning) EmeraldActive.copy(alpha = 0.15f) else DeepSpaceCard,
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
                        .background(if (scanning) EmeraldActive else TextMuted)
                        .alpha(if (scanning) alphaPulse else 1.0f)
                )
                Text(
                    text = if (scanning) "SCANNING" else "STANDBY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = if (scanning) EmeraldActive else TextMuted,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B14)
@Composable
fun RadarScreenPreview() {
    SoundScopeTheme {
        RadarScreen(
            scanning = true,
            statusText = "Scanning BLE and discoverable Classic devices...",
            devices = listOf(
                FoundDevice(
                    address = "AA:BB:CC:DD:EE:01",
                    name = "Sony WH-1000XM5",
                    source = "Bluetooth LE + Classic",
                    rssi = -52,
                    paired = true,
                    services = "00001844-0000-1000-8000-00805f9b34fb, 0000180f-0000-1000-8000-00805f9b34fb",
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
                )
            ),
            filter = "All",
            onFilterChanged = {},
            selectedDevice = null,
            onDeviceSelected = {},
            onStartScan = {},
            onStopScan = {}
        )
    }
}

