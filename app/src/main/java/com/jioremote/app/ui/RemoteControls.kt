package com.jioremote.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun RemoteControls(enabled: Boolean, onKey: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoxWithConstraints(
            Modifier.widthIn(max = 232.dp).fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                .background(colors.surfaceContainerHigh),
        ) {
            val diameter = maxWidth
            Canvas(Modifier.matchParentSize()) {
                for (index in 0..3) {
                    val angle = Math.PI * (0.25 + 0.5 * index)
                    val axis = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    drawLine(
                        color = colors.outlineVariant,
                        start = center + axis * (size.width * 0.24f),
                        end = center + axis * (size.width * 0.46f),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            DirectionButton(Up, enabled, onKey, Modifier.align(Alignment.TopCenter).size(diameter * .43f, diameter * .30f))
            DirectionButton(Down, enabled, onKey, Modifier.align(Alignment.BottomCenter).size(diameter * .43f, diameter * .30f))
            DirectionButton(Left, enabled, onKey, Modifier.align(Alignment.CenterStart).size(diameter * .30f, diameter * .43f))
            DirectionButton(Right, enabled, onKey, Modifier.align(Alignment.CenterEnd).size(diameter * .30f, diameter * .43f))
            FilledIconButton(
                onClick = { onKey(23) },
                enabled = enabled,
                modifier = Modifier.align(Alignment.Center).size(diameter * .40f),
            ) { Text("OK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(RemoteSymbol.Back, "Back", 4, enabled, onKey, Modifier.weight(1f))
            ActionButton(RemoteSymbol.Home, "Home", 3, enabled, onKey, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(RemoteSymbol.Mute, "Mute", 164, enabled, onKey, Modifier.weight(1f))
            ActionButton(RemoteSymbol.VolumeDown, "Volume down", 25, enabled, onKey, Modifier.weight(1f))
            ActionButton(RemoteSymbol.VolumeUp, "Volume up", 24, enabled, onKey, Modifier.weight(1f))
        }
    }
}

private data class Direction(val symbol: RemoteSymbol, val label: String, val code: Int)
private val Up = Direction(RemoteSymbol.Up, "Up", 19)
private val Down = Direction(RemoteSymbol.Down, "Down", 20)
private val Left = Direction(RemoteSymbol.Left, "Left", 21)
private val Right = Direction(RemoteSymbol.Right, "Right", 22)

@Composable
private fun DirectionButton(direction: Direction, enabled: Boolean, onKey: (Int) -> Unit, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    Box(
        modifier.clip(CircleShape)
            .semantics { contentDescription = direction.label }
            .clickable(enabled = enabled, role = Role.Button) { onKey(direction.code) },
        contentAlignment = Alignment.Center,
    ) {
        RemoteIcon(direction.symbol, Modifier.size(28.dp), tint = if (enabled) ink else ink.copy(alpha = .38f))
    }
}

@Composable
private fun ActionButton(
    symbol: RemoteSymbol, label: String, code: Int, enabled: Boolean, onKey: (Int) -> Unit, modifier: Modifier,
) {
    FilledTonalIconButton(
        onClick = { onKey(code) },
        enabled = enabled,
        modifier = modifier.height(50.dp).semantics { contentDescription = label },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) { RemoteIcon(symbol, Modifier.size(23.dp)) }
}

internal enum class RemoteSymbol {
    Tv, Settings, Play, Up, Down, Left, Right, Back, Home, Mute, VolumeDown, VolumeUp,
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
            RemoteSymbol.Up -> drawPath(trace(7 to 14, 12 to 9, 17 to 14), tint, style = stroke)
            RemoteSymbol.Down -> drawPath(trace(7 to 10, 12 to 15, 17 to 10), tint, style = stroke)
            RemoteSymbol.Left -> drawPath(trace(14 to 7, 9 to 12, 14 to 17), tint, style = stroke)
            RemoteSymbol.Right -> drawPath(trace(10 to 7, 15 to 12, 10 to 17), tint, style = stroke)
            RemoteSymbol.Back -> {
                drawPath(trace(11 to 6, 5 to 12, 11 to 18), tint, style = stroke)
                line(5, 12, 20, 12)
            }
            RemoteSymbol.Home -> {
                drawPath(trace(3 to 11, 12 to 3, 21 to 11), tint, style = stroke)
                drawPath(trace(6 to 9, 6 to 21, 10 to 21, 10 to 15, 14 to 15, 14 to 21, 18 to 21, 18 to 9), tint, style = stroke)
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
