package com.soundscope.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundscope.app.ui.theme.AmberGlow
import com.soundscope.app.ui.theme.CrimsonPeak
import com.soundscope.app.ui.theme.NeonCyan
import com.soundscope.app.ui.theme.TextMuted
import com.soundscope.app.ui.theme.TextPrimary
import com.soundscope.app.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun DecibelRingMeter(
    currentDbfs: Double?,
    peakDbfs: Double?,
    active: Boolean,
    modifier: Modifier = Modifier.size(200.dp)
) {
    // Map dBFS (-90 to 0) to 0.0 .. 1.0 fraction
    val safeDbfs = currentDbfs ?: -90.0
    val rawFraction = ((safeDbfs + 90.0) / 90.0).coerceIn(0.0, 1.0).toFloat()

    val animatedFraction by animateFloatAsState(
        targetValue = if (active) rawFraction else 0.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "dbfsMeter"
    )

    val safePeak = peakDbfs ?: -90.0
    val peakFraction = ((safePeak + 90.0) / 90.0).coerceIn(0.0, 1.0).toFloat()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val padding = strokeWidth / 2f + 4.dp.toPx()
            val diameter = size.width - (padding * 2)
            val topLeft = Offset(padding, padding)
            val arcSize = Size(diameter, diameter)

            // 240 degree gauge arc from 150 degrees (bottom-left) to 390 degrees (bottom-right)
            val startAngle = 150f
            val sweepAngle = 240f

            // Background Track Arc
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Progress Gradient Arc
            if (animatedFraction > 0.01f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to NeonCyan,
                        0.5f to AmberGlow,
                        1.0f to CrimsonPeak,
                        center = Offset(size.width / 2f, size.height / 2f)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Peak Hold Marker Dot
            if (active && peakFraction > 0.05f) {
                val peakAngleDeg = startAngle + (sweepAngle * peakFraction)
                val peakRad = Math.toRadians(peakAngleDeg.toDouble())
                val radius = diameter / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val markerX = (centerX + radius * kotlin.math.cos(peakRad)).toFloat()
                val markerY = (centerY + radius * sin(peakRad)).toFloat()

                drawCircle(
                    color = CrimsonPeak,
                    radius = strokeWidth * 0.6f,
                    center = Offset(markerX, markerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = strokeWidth * 0.25f,
                    center = Offset(markerX, markerY)
                )
            }
        }

        // Center Digital Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = currentDbfs?.let { "%.1f".format(it) } ?: "—",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (active) TextPrimary else TextMuted
            )
            Text(
                text = "dBFS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan
            )
            Text(
                text = when {
                    !active -> "MONITOR OFF"
                    safeDbfs < -65 -> "QUIET AMBIENT"
                    safeDbfs < -40 -> "MODERATE SOUND"
                    safeDbfs < -20 -> "LOUD NOISE"
                    else -> "HIGH INTENSITY"
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun AudioWaveformCanvas(
    history: List<Double>,
    active: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth().height(100.dp)
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Grid lines
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.dp.toPx()
        )

        if (history.isEmpty() || !active) {
            // Flat baseline when inactive
            drawLine(
                color = NeonCyan.copy(alpha = 0.2f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }

        // Render Waveform Bars / Spline
        val stepX = width / (history.size.coerceAtLeast(2) - 1).toFloat()
        val path = Path()
        val fillPath = Path()

        fillPath.moveTo(0f, height)

        history.forEachIndexed { index, dbfs ->
            val x = index * stepX
            val normalized = ((dbfs + 90.0) / 90.0).coerceIn(0.05, 1.0).toFloat()
            val barHeight = (height * 0.45f) * normalized
            val y = centerY - barHeight

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevDbfs = history[index - 1]
                val prevNormalized = ((prevDbfs + 90.0) / 90.0).coerceIn(0.05, 1.0).toFloat()
                val prevY = centerY - (height * 0.45f) * prevNormalized

                val controlX1 = prevX + (stepX * 0.5f)
                val controlX2 = x - (stepX * 0.5f)

                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        // Gradient Area Fill under Waveform
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.35f),
                    Color.Transparent
                )
            )
        )

        // Waveform Stroke
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(NeonCyan, AmberGlow, CrimsonPeak)
            ),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
