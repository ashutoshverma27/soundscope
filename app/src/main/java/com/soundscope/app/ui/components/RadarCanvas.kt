package com.soundscope.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundscope.app.FoundDevice
import com.soundscope.app.ui.theme.CyberTeal
import com.soundscope.app.ui.theme.ElectricPurple
import com.soundscope.app.ui.theme.NeonCyan
import com.soundscope.app.ui.theme.TextMuted
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarCanvas(
    scanning: Boolean,
    devices: List<FoundDevice>,
    selectedDevice: FoundDevice?,
    onDeviceSelected: (FoundDevice) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(260.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing)
        ),
        label = "sweep"
    )

    val pulseRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(devices) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = minOf(size.width, size.height) * 0.42f

                        // Find closest device blip to tap point
                        var closest: FoundDevice? = null
                        var minDistance = 40.dp.toPx() // Tap threshold radius

                        devices.forEach { device ->
                            val (blipX, blipY) = calculateBlipPosition(device, center, maxRadius)
                            val distance = (Offset(blipX, blipY) - tapOffset).getDistance()
                            if (distance < minDistance) {
                                minDistance = distance
                                closest = device
                            }
                        }

                        closest?.let { onDeviceSelected(it) }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) * 0.42f
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            // Background concentric rings
            val ringCount = 3
            val ringLabels = listOf("-30 dBm", "-60 dBm", "-90 dBm")

            for (i in 1..ringCount) {
                val radius = maxRadius * (i.toFloat() / ringCount)

                drawCircle(
                    color = CyberTeal.copy(alpha = 0.12f * i),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = if (i == 2) dashedEffect else null)
                )

                // Draw distance label
                val labelText = ringLabels[i - 1]
                val textLayout = textMeasurer.measure(
                    text = labelText,
                    style = TextStyle(
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(center.x + 6.dp.toPx(), center.y - radius - 12.dp.toPx())
                )
            }

            // Crosshair lines
            drawLine(
                color = CyberTeal.copy(alpha = 0.15f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = CyberTeal.copy(alpha = 0.15f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // Scanning pulse ring
            if (scanning) {
                drawCircle(
                    color = NeonCyan.copy(alpha = (1.0f - pulseRadiusFraction) * 0.4f),
                    radius = maxRadius * pulseRadiusFraction,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Sweeping radar beam gradient
            if (scanning) {
                rotate(degrees = sweepAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.75f to Color.Transparent,
                            1.0f to NeonCyan.copy(alpha = 0.35f),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
                    )

                    // Leading edge ray
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.8f),
                        start = center,
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Radar Center Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan, CyberTeal),
                    center = center,
                    radius = 12.dp.toPx()
                ),
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Render Device Blips
            devices.forEach { device ->
                val (blipX, blipY) = calculateBlipPosition(device, center, maxRadius)
                val isSelected = selectedDevice?.address == device.address
                val blipColor = when {
                    isSelected -> ElectricPurple
                    device.audioHint -> NeonCyan
                    device.paired -> CyberTeal
                    else -> ElectricPurple.copy(alpha = 0.8f)
                }

                val blipRadius = if (isSelected) 8.dp.toPx() else 5.dp.toPx()

                // Outer halo glow for audio or selected devices
                if (device.audioHint || isSelected) {
                    drawCircle(
                        color = blipColor.copy(alpha = 0.3f),
                        radius = blipRadius * 2.2f,
                        center = Offset(blipX, blipY)
                    )
                }

                // Core blip dot
                drawCircle(
                    color = blipColor,
                    radius = blipRadius,
                    center = Offset(blipX, blipY)
                )

                // Inner white highlight
                drawCircle(
                    color = Color.White,
                    radius = blipRadius * 0.4f,
                    center = Offset(blipX, blipY)
                )

                // Short label tag
                val nameTag = device.name.take(10)
                if (nameTag.isNotBlank() && nameTag != "Unknown Device") {
                    val labelLayout = textMeasurer.measure(
                        text = nameTag,
                        style = TextStyle(
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(blipX + 8.dp.toPx(), blipY - 6.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun calculateBlipPosition(
    device: FoundDevice,
    center: Offset,
    maxRadius: Float
): Pair<Float, Float> {
    // Map RSSI (-30 dBm close, -95 dBm far) to radius fraction (0.2 to 0.95)
    val rssi = device.rssi ?: -75
    val normalizedRssi = ((rssi + 95).coerceIn(0, 65)) / 65f
    val radiusFraction = 0.95f - (normalizedRssi * 0.75f)
    val radius = maxRadius * radiusFraction

    // Use device address hash code to compute deterministic angle around center
    val angleDegrees = (device.address.hashCode() % 360).let { if (it < 0) it + 360 else it }
    val angleRadians = Math.toRadians(angleDegrees.toDouble())

    val x = center.x + (radius * cos(angleRadians)).toFloat()
    val y = center.y + (radius * sin(angleRadians)).toFloat()

    return Pair(x, y)
}
