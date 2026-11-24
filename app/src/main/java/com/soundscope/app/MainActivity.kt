
package com.soundscope.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private const val VCS_UUID =
    "00001844-0000-1000-8000-00805f9b34fb"

data class FoundDevice(
    val address: String,
    val name: String,
    val source: String,
    val rssi: Int?,
    val paired: Boolean,
    val services: String,
    val manufacturerIds: String
) {
    val audioHint: Boolean
        get() {
            val value = "$name $services".lowercase(Locale.ROOT)
            return listOf(
                "speaker", "soundbar", "audio",
                "headphone", "headset", "jbl", "sony",
                "boat", "bose", "anker", "marshall",
                "harman", "beats", "sennheiser",
                "1844", "184e", "1850"
            ).any { value.contains(it) }
        }

    val advertisesVcs: Boolean
        get() = services.contains(VCS_UUID, ignoreCase = true)
}

class MainActivity : ComponentActivity() {

    private val devices = mutableStateListOf<FoundDevice>()

    private var status by mutableStateOf("Ready to scan.")
    private var scanning by mutableStateOf(false)
    private var filter by mutableStateOf("All")

    private var selectedDevice by mutableStateOf<FoundDevice?>(null)
    private var showConsent by mutableStateOf(false)
    private var gattStatus by mutableStateOf("No inspection performed.")
    private var gattServices by mutableStateOf<List<String>>(emptyList())

    private var activeGatt: BluetoothGatt? = null
    private var receiverRegistered = false

    private var micRunning = false
    private var audioRecord: AudioRecord? = null
    private var currentDbfs by mutableStateOf<Double?>(null)
    private var peakDbfs by mutableStateOf<Double?>(null)
    private var micStatus by mutableStateOf("Microphone is off.")

    private val handler = Handler(Looper.getMainLooper())

    private val stopRunnable = Runnable {
        stopScan("Scan completed.")
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasScanPermission(): Boolean {
        return hasPermission(Manifest.permission.BLUETOOTH_SCAN)
    }

    private fun hasConnectPermission(): Boolean {
        return hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun hasMicrophonePermission(): Boolean {
        return hasPermission(Manifest.permission.RECORD_AUDIO)
    }

    private fun bluetoothPermissions() = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private val bluetoothPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->

            val granted = bluetoothPermissions().all {
                results[it] == true || hasPermission(it)
            }

            if (granted) {
                startScan()
            } else {
                status = "Bluetooth permissions denied."
            }
        }

    private val microphonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted && hasMicrophonePermission()) {
                startNoiseMonitor()
            } else {
                micStatus = "Microphone permission denied."
            }
        }

    // ------------------------------------------------
    // BLUETOOTH CLASSIC RECEIVER
    // ------------------------------------------------

    private val classicReceiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action != BluetoothDevice.ACTION_FOUND) return

            if (!hasConnectPermission()) return

            try {

                val device = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<BluetoothDevice>(
                        BluetoothDevice.EXTRA_DEVICE
                    )
                } ?: return

                val signal = intent.getShortExtra(
                    BluetoothDevice.EXTRA_RSSI,
                    Short.MIN_VALUE
                ).toInt()

                addOrUpdate(
                    address = device.address,
                    name = device.name ?: "Unnamed Classic device",
                    source = "Bluetooth Classic",
                    rssi = signal.takeUnless {
                        it == Short.MIN_VALUE.toInt()
                    },
                    paired = false,
                    services = "",
                    manufacturerIds = ""
                )

            } catch (_: SecurityException) {
                runOnUiThread {
                    status = "Bluetooth permission changed during discovery."
                }
            }
        }
    }

    // ------------------------------------------------
    // BLUETOOTH LE SCANNER
    // ------------------------------------------------

    private val bleCallback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {

            if (!hasConnectPermission()) return

            try {

                val device = result.device
                val record = result.scanRecord

                val name = record?.deviceName
                    ?: device.name
                    ?: "Unnamed BLE device"

                val services = record?.serviceUuids
                    ?.joinToString(", ") {
                        it.uuid.toString()
                    } ?: ""

                val manufacturers =
                    record?.manufacturerSpecificData?.let { data ->

                        (0 until data.size()).joinToString(", ") {
                                index ->
                            "0x%04X".format(data.keyAt(index))
                        }

                    } ?: ""

                addOrUpdate(
                    address = device.address,
                    name = name,
                    source = "Bluetooth LE",
                    rssi = result.rssi,
                    paired = false,
                    services = services,
                    manufacturerIds = manufacturers
                )

            } catch (_: SecurityException) {
                runOnUiThread {
                    status = "Bluetooth permission changed during BLE scan."
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                status = "BLE scan failed: $errorCode"
            }
        }
    }

    // ------------------------------------------------
    // GATT CALLBACK
    // ------------------------------------------------

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            statusCode: Int,
            newState: Int
        ) {

            if (statusCode != BluetoothGatt.GATT_SUCCESS) {

                runOnUiThread {
                    gattStatus = "GATT connection failed: $statusCode"
                }

                closeGattSafely(gatt)
                return
            }

            when (newState) {

                BluetoothProfile.STATE_CONNECTED -> {

                    runOnUiThread {
                        gattStatus = "Connected. Discovering services..."
                    }

                    if (!hasConnectPermission()) {
                        runOnUiThread {
                            gattStatus = "Bluetooth permission unavailable."
                        }
                        closeGattSafely(gatt)
                        return
                    }

                    try {
                        gatt.discoverServices()
                    } catch (_: SecurityException) {
                        runOnUiThread {
                            gattStatus = "Permission denied during discovery."
                        }
                        closeGattSafely(gatt)
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {

                    runOnUiThread {
                        gattStatus = "Disconnected. Inspection finished."
                    }

                    closeGattSafely(gatt)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            statusCode: Int
        ) {

            if (!hasConnectPermission()) {
                runOnUiThread {
                    gattStatus = "Bluetooth permission unavailable."
                }
                closeGattSafely(gatt)
                return
            }

            if (statusCode != BluetoothGatt.GATT_SUCCESS) {

                runOnUiThread {
                    gattStatus = "Service discovery failed: $statusCode"
                }

                closeGattSafely(gatt)
                return
            }

            try {

                val services = gatt.services.map { service ->

                    val uuid = service.uuid.toString()

                    "${serviceLabel(uuid)}\n$uuid"
                }

                val hasVcs = gatt.services.any {
                    it.uuid.toString().equals(
                        VCS_UUID,
                        ignoreCase = true
                    )
                }

                runOnUiThread {

                    gattServices = services

                    gattStatus = if (hasVcs) {
                        "VCS found. Potential standard volume-control support."
                    } else {
                        "Inspection complete. VCS not found."
                    }
                }

                // Service discovery only. No writes.
                closeGattSafely(gatt)

            } catch (_: SecurityException) {

                runOnUiThread {
                    gattStatus = "Permission denied during service inspection."
                }

                closeGattSafely(gatt)
            }
        }
    }

    // ------------------------------------------------
    // GATT SAFE CLEANUP
    // ------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun closeGattSafely(gatt: BluetoothGatt) {

        if (!hasConnectPermission()) return

        try {
            gatt.disconnect()
        } catch (_: SecurityException) {
        }

        try {
            gatt.close()
        } catch (_: SecurityException) {
        }

        if (activeGatt === gatt) {
            activeGatt = null
        }
    }

    // ------------------------------------------------
    // MAIN UI
    // ------------------------------------------------


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navy = Color(0xFF0B1220)
        val panel = Color(0xFF142235)
        val teal = Color(0xFF5EEAD4)
        val muted = Color(0xFF94A3B8)

        setContent {

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = teal,
                    onPrimary = navy,
                    secondary = Color(0xFF38BDF8),
                    background = navy,
                    surface = panel,
                    onSurface = Color(0xFFF1F5F9)
                )
            ) {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = navy
                ) {

                    val visibleDevices = devices.filter {
                        when (filter) {
                            "Audio" -> it.audioHint
                            "Paired" -> it.paired
                            else -> true
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 18.dp,
                            end = 18.dp,
                            top = 22.dp,
                            bottom = 28.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // HEADER

                        item {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF123B48)
                                    ) {
                                        Text(
                                            "⌁",
                                            modifier = Modifier.padding(
                                                horizontal = 13.dp,
                                                vertical = 5.dp
                                            ),
                                            color = teal,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column {
                                        Text(
                                            "SoundScope",
                                            fontSize = 25.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Text(
                                            "BLUETOOTH INTELLIGENCE",
                                            fontSize = 10.sp,
                                            letterSpacing = 1.5.sp,
                                            color = muted
                                        )
                                    }
                                }

                                Surface(
                                    color = if (scanning)
                                        Color(0xFF164E42)
                                    else
                                        Color(0xFF26374C),
                                    shape = RoundedCornerShape(50)
                                ) {

                                    Text(
                                        if (scanning) "● SCANNING" else "● READY",
                                        modifier = Modifier.padding(
                                            horizontal = 11.dp,
                                            vertical = 7.dp
                                        ),
                                        color = if (scanning) teal else muted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // SCANNER PANEL

                        item {

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = panel
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {

                                    Text(
                                        "BLUETOOTH SCANNER",
                                        color = teal,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "Discover nearby devices",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "Scan Bluetooth Classic and BLE advertisements around you.",
                                        color = muted,
                                        fontSize = 13.sp
                                    )

                                    Text(
                                        status,
                                        color = muted,
                                        fontSize = 12.sp
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {

                                        Button(
                                            onClick = {
                                                checkPermissionsAndScan()
                                            },
                                            enabled = !scanning,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                "Start scanning",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                stopScan("Scan stopped.")
                                            },
                                            enabled = scanning,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Stop")
                                        }
                                    }
                                }
                            }
                        }

                        // STATISTICS

                        item {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {

                                StatCard(
                                    "DEVICES",
                                    devices.size.toString(),
                                    Modifier.weight(1f),
                                    teal
                                )

                                StatCard(
                                    "AUDIO-LIKE",
                                    devices.count { it.audioHint }.toString(),
                                    Modifier.weight(1f),
                                    Color(0xFF38BDF8)
                                )

                                StatCard(
                                    "PAIRED",
                                    devices.count { it.paired }.toString(),
                                    Modifier.weight(1f),
                                    Color(0xFFA78BFA)
                                )
                            }
                        }

                        // FILTERS

                        item {

                            Column(
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {

                                Text(
                                    "Nearby devices",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {

                                    FilterChip(
                                        selected = filter == "All",
                                        onClick = { filter = "All" },
                                        label = { Text("All") }
                                    )

                                    FilterChip(
                                        selected = filter == "Audio",
                                        onClick = { filter = "Audio" },
                                        label = { Text("Audio-like") }
                                    )

                                    FilterChip(
                                        selected = filter == "Paired",
                                        onClick = { filter = "Paired" },
                                        label = { Text("Paired") }
                                    )
                                }

                                Text(
                                    "${visibleDevices.size} device records found",
                                    color = muted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // DEVICE LIST

                        if (visibleDevices.isEmpty()) {

                            item {

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = panel
                                    )
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {

                                        Text(
                                            "⌁",
                                            fontSize = 35.sp,
                                            color = teal
                                        )

                                        Text(
                                            "No devices discovered yet",
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Text(
                                            "Start a scan to discover available Bluetooth devices.",
                                            color = muted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                        } else {

                            items(
                                visibleDevices,
                                key = { it.address }
                            ) { device ->

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(17.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = panel
                                    )
                                ) {

                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {

                                            Surface(
                                                color = Color(0xFF20364A),
                                                shape = RoundedCornerShape(11.dp)
                                            ) {

                                                Text(
                                                    "ᛒ",
                                                    modifier = Modifier.padding(
                                                        horizontal = 12.dp,
                                                        vertical = 7.dp
                                                    ),
                                                    color = teal,
                                                    fontSize = 23.sp
                                                )
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {

                                                Text(
                                                    device.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )

                                                Text(
                                                    device.source,
                                                    color = muted,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Text(
                                                device.rssi?.let {
                                                    "$it dBm"
                                                } ?: "—",
                                                color = teal,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        HorizontalDivider(
                                            color = Color(0xFF26374C)
                                        )

                                        Text(
                                            "Address: ${device.address}",
                                            color = muted,
                                            fontSize = 11.sp
                                        )

                                        if (device.paired) {
                                            DeviceTag("PAIRED", Color(0xFFA78BFA))
                                        }

                                        if (device.audioHint) {
                                            DeviceTag("POSSIBLE AUDIO DEVICE", teal)
                                        }

                                        if (device.advertisesVcs) {
                                            DeviceTag("VCS ADVERTISED", Color(0xFF38BDF8))
                                        }

                                        if (device.services.isNotBlank()) {
                                            Text(
                                                "Services: ${device.services}",
                                                color = muted,
                                                fontSize = 10.sp
                                            )
                                        }

                                        if (device.manufacturerIds.isNotBlank()) {
                                            Text(
                                                "Manufacturer IDs: ${device.manufacturerIds}",
                                                color = muted,
                                                fontSize = 10.sp
                                            )
                                        }

                                        if (device.source.contains("Bluetooth LE")) {

                                            OutlinedButton(
                                                onClick = {
                                                    selectedDevice = device
                                                    showConsent = true
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Inspect GATT services")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // GATT PANEL

                        item {

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = panel
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(15.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    Text(
                                        "DEVICE INVESTIGATION",
                                        color = teal,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.3.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        gattStatus,
                                        fontSize = 13.sp
                                    )

                                    gattServices.forEach {
                                        Text(
                                            it,
                                            color = muted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // NOISE MONITOR

                        item {

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = panel
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {

                                        Text(
                                            "ROOM NOISE MONITOR",
                                            color = teal,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.3.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            if (micRunning) "● ACTIVE" else "○ OFF",
                                            color = if (micRunning) teal else muted,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Text(
                                        currentDbfs?.let {
                                            "%.1f dBFS".format(it)
                                        } ?: "—",
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "Current relative microphone level",
                                        color = muted,
                                        fontSize = 11.sp
                                    )

                                    Text(
                                        peakDbfs?.let {
                                            "Peak: %.1f dBFS".format(it)
                                        } ?: "Peak: —",
                                        color = muted,
                                        fontSize = 12.sp
                                    )

                                    Text(
                                        micStatus,
                                        color = muted,
                                        fontSize = 11.sp
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                                    ) {

                                        Button(
                                            onClick = {
                                                requestMicrophone()
                                            },
                                            enabled = !micRunning,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Start monitor")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                stopNoiseMonitor()
                                            },
                                            enabled = micRunning,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Stop")
                                        }
                                    }
                                }
                            }
                        }

                        // ACTIONS

                        item {

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {

                                OutlinedButton(
                                    onClick = { shareReport() },
                                    enabled = devices.isNotEmpty(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Export report")
                                }

                                OutlinedButton(
                                    onClick = {
                                        devices.clear()
                                        status = "Results cleared."
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Clear results")
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "SoundScope discovers and investigates Bluetooth devices. " +
                                        "It does not interfere with third-party equipment. " +
                                        "Microphone readings are relative dBFS, not calibrated dB(A).",
                                color = muted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // AUTHORIZATION DIALOG

                    if (showConsent) {

                        AlertDialog(
                            onDismissRequest = {
                                showConsent = false
                            },

                            title = {
                                Text("Authorized device inspection")
                            },

                            text = {
                                Text(
                                    "Continue only if you own this device or " +
                                            "have permission to inspect it. " +
                                            "Only GATT services will be enumerated."
                                )
                            },

                            confirmButton = {

                                TextButton(
                                    onClick = {
                                        showConsent = false
                                        inspectSelectedDevice()
                                    }
                                ) {
                                    Text("Inspect")
                                }
                            },

                            dismissButton = {

                                TextButton(
                                    onClick = {
                                        showConsent = false
                                    }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

// Reusable statistics card

    @Composable
    private fun StatCard(
        label: String,
        value: String,
        modifier: Modifier = Modifier,
        accent: Color
    ) {

        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF142235)
            )
        ) {

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Text(
                    value,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )

                Text(
                    label,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.7.sp
                )
            }
        }
    }

// Reusable device status tag

    @Composable
    private fun DeviceTag(
        label: String,
        accent: Color
    ) {

        Surface(
            color = accent.copy(alpha = 0.12f),
            shape = RoundedCornerShape(6.dp)
        ) {

            Text(
                label,
                modifier = Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 5.dp
                ),
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }


    // ------------------------------------------------
    // SERVICE UUID LABELS
    // ------------------------------------------------

    private fun serviceLabel(uuid: String): String {

        return when (uuid.lowercase(Locale.ROOT)) {

            VCS_UUID -> "Volume Control Service"

            "0000180a-0000-1000-8000-00805f9b34fb" ->
                "Device Information Service"

            "0000180f-0000-1000-8000-00805f9b34fb" ->
                "Battery Service"

            "00001812-0000-1000-8000-00805f9b34fb" ->
                "Human Interface Device Service"

            "0000184e-0000-1000-8000-00805f9b34fb" ->
                "Audio Stream Control Service"

            "00001850-0000-1000-8000-00805f9b34fb" ->
                "Published Audio Capabilities Service"

            else -> "GATT Service"
        }
    }

    // ------------------------------------------------
    // BLUETOOTH PERMISSION CHECK
    // ------------------------------------------------

    private fun checkPermissionsAndScan() {

        val missing = bluetoothPermissions().filter {
            !hasPermission(it)
        }

        if (missing.isEmpty()) {
            startScan()
        } else {
            bluetoothPermissionLauncher.launch(
                missing.toTypedArray()
            )
        }
    }

    // ------------------------------------------------
    // START BLUETOOTH SCAN
    // ------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun startScan() {

        if (!hasScanPermission() || !hasConnectPermission()) {
            status = "Bluetooth permissions are required."
            return
        }

        try {

            val adapter =
                (getSystemService(Context.BLUETOOTH_SERVICE)
                        as BluetoothManager).adapter

            if (adapter == null) {
                status = "Bluetooth adapter unavailable."
                return
            }

            if (!adapter.isEnabled) {
                status = "Enable Bluetooth first."
                return
            }

            stopScan("Refreshing results...")

            devices.clear()
            gattServices = emptyList()
            gattStatus = "No inspection performed."

            adapter.bondedDevices.forEach { paired ->

                addOrUpdate(
                    paired.address,
                    paired.name ?: "Unnamed paired device",
                    "Paired-device list",
                    null,
                    true,
                    "",
                    ""
                )
            }

            ContextCompat.registerReceiver(
                this,
                classicReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND),
                ContextCompat.RECEIVER_EXPORTED
            )

            receiverRegistered = true

            adapter.cancelDiscovery()

            val classicStarted = adapter.startDiscovery()

            val scanner = adapter.bluetoothLeScanner

            if (scanner == null) {
                status = "BLE scanner unavailable."
                stopScan(status)
                return
            }

            scanner.startScan(
                null,
                ScanSettings.Builder()
                    .setScanMode(
                        ScanSettings.SCAN_MODE_LOW_LATENCY
                    )
                    .build(),
                bleCallback
            )

            scanning = true

            status = if (classicStarted) {
                "Scanning BLE and discoverable Classic devices..."
            } else {
                "BLE scan started; Classic discovery could not start."
            }

            handler.postDelayed(
                stopRunnable,
                20_000
            )

        } catch (e: SecurityException) {

            stopScan("Bluetooth permission denied.")

        } catch (e: Exception) {

            stopScan("Scan error: ${e.message}")
        }
    }

    // ------------------------------------------------
    // DEVICE UPDATE
    // ------------------------------------------------

    private fun addOrUpdate(
        address: String,
        name: String,
        source: String,
        rssi: Int?,
        paired: Boolean,
        services: String,
        manufacturerIds: String
    ) {

        runOnUiThread {

            val index = devices.indexOfFirst {
                it.address == address
            }

            if (index >= 0) {

                val old = devices[index]

                devices[index] = old.copy(

                    name = if (name.startsWith("Unnamed")) {
                        old.name
                    } else {
                        name
                    },

                    source = (
                            old.source.split(" + ") + source
                            ).distinct().joinToString(" + "),

                    rssi = rssi ?: old.rssi,

                    paired = old.paired || paired,

                    services = listOf(old.services, services)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(", "),

                    manufacturerIds = listOf(
                        old.manufacturerIds,
                        manufacturerIds
                    )
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(", ")
                )

            } else {

                devices.add(
                    FoundDevice(
                        address,
                        name,
                        source,
                        rssi,
                        paired,
                        services,
                        manufacturerIds
                    )
                )
            }
        }
    }

    // ------------------------------------------------
    // STOP BLUETOOTH SCAN
    // ------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun stopScan(message: String) {

        handler.removeCallbacks(stopRunnable)

        try {

            if (hasScanPermission()) {

                val adapter =
                    (getSystemService(Context.BLUETOOTH_SERVICE)
                            as BluetoothManager).adapter

                adapter?.cancelDiscovery()
                adapter?.bluetoothLeScanner?.stopScan(bleCallback)
            }

        } catch (_: SecurityException) {
        }

        if (receiverRegistered) {

            try {
                unregisterReceiver(classicReceiver)
            } catch (_: Exception) {
            }

            receiverRegistered = false
        }

        scanning = false
        status = message
    }

    // ------------------------------------------------
    // GATT INSPECTION
    // ------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun inspectSelectedDevice() {

        if (!hasConnectPermission()) {
            gattStatus = "Bluetooth Connect permission required."
            return
        }

        val selected = selectedDevice ?: return

        try {

            val adapter =
                (getSystemService(Context.BLUETOOTH_SERVICE)
                        as BluetoothManager).adapter

            val device = adapter.getRemoteDevice(selected.address)

            activeGatt?.let {
                closeGattSafely(it)
            }

            gattServices = emptyList()

            gattStatus = "Connecting for read-only service discovery..."

            activeGatt = device.connectGatt(
                this,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )

        } catch (e: SecurityException) {

            gattStatus = "Bluetooth permission denied."

        } catch (e: Exception) {

            gattStatus = "GATT error: ${e.message}"
        }
    }

    // ------------------------------------------------
    // MICROPHONE
    // ------------------------------------------------

    private fun requestMicrophone() {

        if (hasMicrophonePermission()) {
            startNoiseMonitor()
        } else {
            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startNoiseMonitor() {

        if (!hasMicrophonePermission()) {
            micStatus = "Microphone permission required."
            return
        }

        if (micRunning) return

        val sampleRate = 44100

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBuffer <= 0) {
            micStatus = "Microphone unavailable."
            return
        }

        try {

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, sampleRate / 2)
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                micStatus = "Microphone initialization failed."
                return
            }

            audioRecord = recorder

            recorder.startRecording()

            micRunning = true
            currentDbfs = null
            peakDbfs = null

            micStatus = "Microphone active."

            Thread {

                val buffer = ShortArray(2048)

                while (micRunning) {

                    val count = try {
                        recorder.read(buffer, 0, buffer.size)
                    } catch (_: Exception) {
                        -1
                    }

                    if (count <= 0) break

                    var sum = 0.0

                    for (i in 0 until count) {

                        val value = buffer[i] / 32768.0

                        sum += value * value
                    }

                    val rms = sqrt(sum / count)

                    val dbfs = 20.0 * log10(
                        rms.coerceAtLeast(1e-6)
                    )

                    runOnUiThread {

                        currentDbfs = dbfs

                        peakDbfs = maxOf(
                            peakDbfs ?: -120.0,
                            dbfs
                        )
                    }
                }

            }.start()

        } catch (e: SecurityException) {

            micRunning = false
            micStatus = "Microphone permission denied."

        } catch (e: Exception) {

            micRunning = false
            micStatus = "Microphone error: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopNoiseMonitor() {

        micRunning = false

        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }

        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }

        audioRecord = null

        currentDbfs = null
        peakDbfs = null

        micStatus = "Microphone is off."
    }

    // ------------------------------------------------
    // EXPORT REPORT
    // ------------------------------------------------

    private fun shareReport() {

        val report = buildString {

            appendLine("SoundScope scan report")
            appendLine()

            devices.forEach {

                appendLine("Name: ${it.name}")
                appendLine("Address: ${it.address}")
                appendLine("Source: ${it.source}")
                appendLine("RSSI: ${it.rssi ?: "Unavailable"}")
                appendLine("Paired: ${it.paired}")
                appendLine("Services: ${it.services}")
                appendLine("Manufacturer IDs: ${it.manufacturerIds}")
                appendLine()
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {

            type = "text/plain"

            putExtra(
                Intent.EXTRA_SUBJECT,
                "SoundScope report"
            )

            putExtra(Intent.EXTRA_TEXT, report)
        }

        startActivity(
            Intent.createChooser(
                intent,
                "Export report"
            )
        )
    }

    // ------------------------------------------------
    // CLEANUP
    // ------------------------------------------------

    override fun onDestroy() {

        stopScan("App closed.")
        stopNoiseMonitor()

        activeGatt?.let {
            closeGattSafely(it)
        }

        activeGatt = null

        super.onDestroy()
    }
}
