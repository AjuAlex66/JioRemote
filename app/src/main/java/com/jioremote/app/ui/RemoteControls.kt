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
import com.jioremote.app.adb.TvPowerAction
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun BottomControls(
    enabled: Boolean,
    onKey: (Int) -> Unit,
    onPowerAction: (TvPowerAction) -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BottomControl(RemoteSymbol.Mute, "Mute", enabled) { onKey(164) }
                    BottomControl(RemoteSymbol.VolumeDown, "Volume down", enabled) { onKey(25) }
                    BottomControl(RemoteSymbol.VolumeUp, "Volume up", enabled) { onKey(24) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BottomControl(RemoteSymbol.Reboot, "Reboot TV", enabled) { onPowerAction(TvPowerAction.Reboot) }
                    BottomControl(RemoteSymbol.Power, "Power Off TV", enabled) { onPowerAction(TvPowerAction.PowerOff) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomControl(symbol: RemoteSymbol, label: String, enabled: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    FilledTonalButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RemoteIcon(symbol, Modifier.size(21.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

internal enum class RemoteSymbol {
    Tv, Settings, Play, Drag, Mute, VolumeDown, VolumeUp, Reboot, Power,
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
            RemoteSymbol.Reboot -> {
                drawArc(tint, 212f, 303f, false, point(4, 4), Size(16 * unit, 16 * unit), style = stroke)
                drawPath(trace(3 to 4, 4 to 9, 9 to 8), tint, style = stroke)
            }
            RemoteSymbol.Power -> {
                drawArc(tint, -48f, 276f, false, point(4, 5), Size(16 * unit, 16 * unit), style = stroke)
                line(12, 2, 12, 11)
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
