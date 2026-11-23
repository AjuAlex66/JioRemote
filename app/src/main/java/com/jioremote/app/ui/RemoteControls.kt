package com.jioremote.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun VolumeBar(enabled: Boolean, onKey: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Surface(tonalElevation = 3.dp) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val controls = listOf(
                    Triple(RemoteSymbol.Mute, "Mute", 164),
                    Triple(RemoteSymbol.VolumeDown, "Volume down", 25),
                    Triple(RemoteSymbol.VolumeUp, "Volume up", 24),
                )
                for ((icon, label, code) in controls) {
                    FilledTonalButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onKey(code)
                        },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RemoteIcon(icon, Modifier.size(21.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

internal enum class RemoteSymbol {
    Tv, Settings, Play, Drag, Mute, VolumeDown, VolumeUp,
}

@Composable
internal fun RemoteIcon(symbol: RemoteSymbol, modifier: Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier) {
        val unit = size.minDimension / 24
        val stroke = Stroke(width = unit * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun point(x: Int, y: Int) = Offset(x * unit, y * unit)
        fun line(x1: Int, y1: Int, x2: Int, y2: Int) =
            drawLine(tint, point(x1, y1), point(x2, y2), stroke.width, StrokeCap.Round)
        fun trace(vararg points: Pair<Int, Int>) = Path().apply {
            moveTo(points[0].first * unit, points[0].second * unit)
            points.drop(1).forEach { (x, y) -> lineTo(x * unit, y * unit) }
        }
        when (symbol) {
            RemoteSymbol.Tv -> {
                drawRoundRect(tint, point(2, 4), Size(20 * unit, 14 * unit), CornerRadius(3 * unit), style = stroke)
                line(8, 22, 16, 22)
            }
            RemoteSymbol.Settings -> {
                drawCircle(tint, 6 * unit, center, style = stroke)
                drawCircle(tint, 2 * unit, center, style = stroke)
                for (index in 0..7) {
                    val angle = Math.PI * index / 4
                    val axis = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    drawLine(tint, center + axis * (unit * 6), center + axis * (unit * 9), stroke.width, StrokeCap.Round)
                }
            }
            RemoteSymbol.Play -> drawPath(trace(7 to 3, 21 to 12, 7 to 21).apply { close() }, tint)
            RemoteSymbol.Drag -> {
                for (x in listOf(9, 15)) for (y in listOf(6, 12, 18)) {
                    drawCircle(tint, unit * 1.35f, point(x, y))
                }
            }
            RemoteSymbol.Mute, RemoteSymbol.VolumeDown, RemoteSymbol.VolumeUp -> {
                drawPath(trace(3 to 9, 6 to 9, 11 to 5, 11 to 19, 6 to 15, 3 to 15).apply { close() }, tint, style = stroke)
                if (symbol == RemoteSymbol.Mute) {
                    line(16, 9, 22, 15)
                    line(22, 9, 16, 15)
                } else {
                    line(15, 12, 22, 12)
                    if (symbol == RemoteSymbol.VolumeUp) line(18, 9, 18, 15)
                }
            }
        }
    }
}
