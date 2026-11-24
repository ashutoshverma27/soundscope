package com.soundscope.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.soundscope.app.ui.components.BottomNavDock
import com.soundscope.app.ui.components.NavTab
import com.soundscope.app.ui.screens.AnalyticsScreen
import com.soundscope.app.ui.screens.AudioScreen
import com.soundscope.app.ui.screens.InspectorScreen
import com.soundscope.app.ui.screens.RadarScreen
import com.soundscope.app.ui.theme.MidnightBackground
import com.soundscope.app.ui.theme.NeonCyan
import com.soundscope.app.ui.theme.SoundScopeTheme
import com.soundscope.app.ui.theme.TextMuted
import com.soundscope.app.ui.theme.TextPrimary
import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt

private const val VCS_UUID = "00001844-0000-1000-8000-00805f9b34fb"

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

    private var micRunning by mutableStateOf(false)
    private var audioRecord: AudioRecord? = null
    private var currentDbfs by mutableStateOf<Double?>(null)
    private var peakDbfs by mutableStateOf<Double?>(null)
    private var micStatus by mutableStateOf("Microphone is off.")
    private val dbfsHistory = mutableStateListOf<Double>()

    private var currentTab by mutableStateOf(NavTab.RADAR)

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
                    rssi = signal.takeUnless { it == Short.MIN_VALUE.toInt() },
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
                    ?.joinToString(", ") { it.uuid.toString() } ?: ""

                val manufacturers =
                    record?.manufacturerSpecificData?.let { data ->
                        (0 until data.size()).joinToString(", ") { index ->
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
                    it.uuid.toString().equals(VCS_UUID, ignoreCase = true)
                }

                runOnUiThread {
                    gattServices = services
                    gattStatus = if (hasVcs) {
                        "VCS found. Potential standard volume-control support."
                    } else {
                        "Inspection complete. VCS not found."
                    }
                }

                closeGattSafely(gatt)
            } catch (_: SecurityException) {
                runOnUiThread {
                    gattStatus = "Permission denied during service inspection."
                }
                closeGattSafely(gatt)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGattSafely(gatt: BluetoothGatt) {
        if (!hasConnectPermission()) return
        try { gatt.disconnect() } catch (_: SecurityException) {}
        try { gatt.close() } catch (_: SecurityException) {}
        if (activeGatt === gatt) {
            activeGatt = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SoundScopeTheme {
                Scaffold(
                    bottomBar = {
                        BottomNavDock(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    },
                    containerColor = MidnightBackground
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MidnightBackground)
                            .padding(paddingValues)
                    ) {
                        Crossfade(
                            targetState = currentTab,
                            animationSpec = tween(300),
                            label = "tabTransition"
                        ) { tab ->
                            when (tab) {
                                NavTab.RADAR -> RadarScreen(
                                    scanning = scanning,
                                    statusText = status,
                                    devices = devices,
                                    filter = filter,
                                    onFilterChanged = { filter = it },
                                    selectedDevice = selectedDevice,
                                    onDeviceSelected = { device ->
                                        selectedDevice = device
                                        showConsent = true
                                    },
                                    onStartScan = { checkPermissionsAndScan() },
                                    onStopScan = { stopScan("Scan stopped.") }
                                )

                                NavTab.MONITOR -> AudioScreen(
                                    micRunning = micRunning,
                                    currentDbfs = currentDbfs,
                                    peakDbfs = peakDbfs,
                                    micStatus = micStatus,
                                    dbfsHistory = dbfsHistory,
                                    onRequestMic = { requestMicrophone() },
                                    onStopMic = { stopNoiseMonitor() }
                                )

                                NavTab.INSPECTOR -> InspectorScreen(
                                    selectedDevice = selectedDevice,
                                    gattStatus = gattStatus,
                                    gattServices = gattServices,
                                    onInspectClick = { inspectSelectedDevice() }
                                )

                                NavTab.ANALYTICS -> AnalyticsScreen(
                                    devices = devices,
                                    onExportReport = { shareReport() },
                                    onClearResults = {
                                        devices.clear()
                                        status = "Results cleared."
                                    }
                                )
                            }
                        }
                    }

                    // CONSENT DIALOG
                    if (showConsent) {
                        AlertDialog(
                            onDismissRequest = { showConsent = false },
                            title = {
                                Text(
                                    text = "AUTHORIZED DEVICE INSPECTION",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            },
                            text = {
                                Text(
                                    text = "Continue only if you own this device (${selectedDevice?.name ?: "Selected Device"}) or have permission to inspect it. Read-only GATT services will be enumerated.",
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showConsent = false
                                        currentTab = NavTab.INSPECTOR
                                        inspectSelectedDevice()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                                ) {
                                    Text("INSPECT GATT", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showConsent = false },
                                    colors = ButtonDefaults.textButtonColors(contentColor = TextMuted)
                                ) {
                                    Text("CANCEL")
                                }
                            },
                            containerColor = MidnightBackground
                        )
                    }
                }
            }
        }
    }

    private fun serviceLabel(uuid: String): String {
        return when (uuid.lowercase(Locale.ROOT)) {
            VCS_UUID -> "Volume Control Service"
            "0000180a-0000-1000-8000-00805f9b34fb" -> "Device Information Service"
            "0000180f-0000-1000-8000-00805f9b34fb" -> "Battery Service"
            "00001812-0000-1000-8000-00805f9b34fb" -> "Human Interface Device Service"
            "0000184e-0000-1000-8000-00805f9b34fb" -> "Audio Stream Control Service"
            "00001850-0000-1000-8000-00805f9b34fb" -> "Published Audio Capabilities Service"
            else -> "GATT Service"
        }
    }

    private fun checkPermissionsAndScan() {
        val missing = bluetoothPermissions().filter { !hasPermission(it) }
        if (missing.isEmpty()) {
            startScan()
        } else {
            bluetoothPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (!hasScanPermission() || !hasConnectPermission()) {
            status = "Bluetooth permissions are required."
            return
        }

        try {
            val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
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
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build(),
                bleCallback
            )

            scanning = true

            status = if (classicStarted) {
                "Scanning BLE and discoverable Classic devices..."
            } else {
                "BLE scan started; Classic discovery could not start."
            }

            handler.postDelayed(stopRunnable, 20_000)

        } catch (e: SecurityException) {
            stopScan("Bluetooth permission denied.")
        } catch (e: Exception) {
            stopScan("Scan error: ${e.message}")
        }
    }

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
            val index = devices.indexOfFirst { it.address == address }
            if (index >= 0) {
                val old = devices[index]
                devices[index] = old.copy(
                    name = if (name.startsWith("Unnamed")) old.name else name,
                    source = (old.source.split(" + ") + source).distinct().joinToString(" + "),
                    rssi = rssi ?: old.rssi,
                    paired = old.paired || paired,
                    services = listOf(old.services, services).filter { it.isNotBlank() }.distinct().joinToString(", "),
                    manufacturerIds = listOf(old.manufacturerIds, manufacturerIds).filter { it.isNotBlank() }.distinct().joinToString(", ")
                )
            } else {
                devices.add(
                    FoundDevice(address, name, source, rssi, paired, services, manufacturerIds)
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan(message: String) {
        handler.removeCallbacks(stopRunnable)
        try {
            if (hasScanPermission()) {
                val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                adapter?.cancelDiscovery()
                adapter?.bluetoothLeScanner?.stopScan(bleCallback)
            }
        } catch (_: SecurityException) {}

        if (receiverRegistered) {
            try {
                unregisterReceiver(classicReceiver)
            } catch (_: Exception) {}
            receiverRegistered = false
        }

        scanning = false
        status = message
    }

    @SuppressLint("MissingPermission")
    private fun inspectSelectedDevice() {
        if (!hasConnectPermission()) {
            gattStatus = "Bluetooth Connect permission required."
            return
        }

        val selected = selectedDevice ?: return

        try {
            val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            val device = adapter.getRemoteDevice(selected.address)

            activeGatt?.let { closeGattSafely(it) }

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

    private fun requestMicrophone() {
        if (hasMicrophonePermission()) {
            startNoiseMonitor()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
                    val dbfs = 20.0 * log10(rms.coerceAtLeast(1e-6))

                    runOnUiThread {
                        currentDbfs = dbfs
                        peakDbfs = maxOf(peakDbfs ?: -120.0, dbfs)

                        if (dbfsHistory.size >= 30) {
                            dbfsHistory.removeAt(0)
                        }
                        dbfsHistory.add(dbfs)
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
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
        currentDbfs = null
        peakDbfs = null
        dbfsHistory.clear()
        micStatus = "Microphone is off."
    }

    private fun shareReport() {
        val report = buildString {
            appendLine("SoundScope Scan Report")
            appendLine("Generated by SoundScope Intelligence")
            appendLine("----------------------------------------")
            appendLine()

            devices.forEach {
                appendLine("Device Name: ${it.name}")
                appendLine("MAC Address: ${it.address}")
                appendLine("Protocol: ${it.source}")
                appendLine("Signal Strength: ${it.rssi?.let { r -> "$r dBm" } ?: "Unavailable"}")
                appendLine("Paired: ${it.paired}")
                appendLine("Audio Capable: ${it.audioHint}")
                appendLine("Advertised Services: ${it.services}")
                appendLine("Manufacturer Data: ${it.manufacturerIds}")
                appendLine("----------------------------------------")
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SoundScope Intelligence Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }

        startActivity(Intent.createChooser(intent, "Export Report"))
    }

    override fun onDestroy() {
        stopScan("App closed.")
        stopNoiseMonitor()
        activeGatt?.let { closeGattSafely(it) }
        activeGatt = null
        super.onDestroy()
    }
}
