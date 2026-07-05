package com.gal.myhome.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gal.myhome.DashboardViewModel
import com.gal.myhome.TileUi
import kotlin.math.roundToInt

private class SeriesDef(val suffix: String, val label: String, val unit: String, val color: Color)

private val SERIES = listOf(
    SeriesDef("temp", "Temperature", "°C", Color(0xFFE8794A)),
    SeriesDef("humidity", "Humidity", "%", Color(0xFF4A9BD9)),
    SeriesDef("pm25", "PM2.5", " µg/m³", Color(0xFF34A853)),
)

/* 24h chart for a sensor tile — data comes from the Pi's 5-minute samples */
@Composable
fun HistorySheet(tile: TileUi, vm: DashboardViewModel, onClose: () -> Unit) {
    var data by remember(tile.key) {
        mutableStateOf<Map<String, List<Pair<Long, Double>>>?>(null)
    }
    var failed by remember(tile.key) { mutableStateOf(false) }
    LaunchedEffect(tile.key) {
        try {
            data = vm.history()
        } catch (_: Exception) {
            failed = true
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Dialog windows pop in with no transition of their own — animate the
        // card in with a springy fade + scale + rise from the tapped tile
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val appear by animateFloatAsState(
            if (shown) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "historyAppear",
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 3.dp,
            modifier = Modifier
                .width(620.dp)
                .graphicsLayer {
                    // the spring overshoots past 1 for the pop — keep alpha legal
                    alpha = appear.coerceIn(0f, 1f)
                    scaleX = 0.9f + 0.1f * appear
                    scaleY = 0.9f + 0.1f * appear
                    translationY = (1f - appear) * 28.dp.toPx()
                },
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${tile.name} · last 24 h",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "close") }
                }
                val snapshot = data
                when {
                    failed -> Text(
                        "Couldn't load history from the server.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 40.dp),
                    )
                    snapshot == null -> Box(
                        Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                    else -> {
                        val available = SERIES.mapNotNull { def ->
                            tile.origNames
                                .firstNotNullOfOrNull { n -> snapshot["$n|${def.suffix}"] }
                                ?.let { def to it }
                        }
                        if (available.isEmpty()) {
                            Text(
                                "No history for this device yet — the server records a sample every 5 minutes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 40.dp),
                            )
                        } else {
                            var sel by remember(available.size) { mutableIntStateOf(0) }
                            if (available.size > 1) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    available.forEachIndexed { i, (def, _) ->
                                        FilterChip(
                                            selected = i == sel,
                                            onClick = { sel = i },
                                            label = { Text(def.label) },
                                        )
                                    }
                                }
                            }
                            val (def, pts) = available[sel]
                            HistoryChart(pts, def.color, def.unit)
                        }
                    }
                }
            }
        }
    }
}

private fun fmtVal(v: Double): String {
    val r = (v * 10).roundToInt() / 10.0
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}

@Composable
private fun HistoryChart(all: List<Pair<Long, Double>>, color: Color, unit: String) {
    val now = remember(all) { System.currentTimeMillis() }
    val from = now - 24L * 3600 * 1000
    val pts = remember(all) { all.filter { it.first >= from }.sortedBy { it.first } }
    if (pts.size < 2) {
        Text(
            "Not enough data yet — check back in a little while.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 40.dp),
        )
        return
    }
    val minV = pts.minOf { it.second }
    val maxV = pts.maxOf { it.second }
    val span = ((maxV - minV) * 0.2).coerceAtLeast(0.5)
    val lo = minV - span
    val hi = maxV + span
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.padding(top = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "low ${fmtVal(minV)}$unit",
                style = MaterialTheme.typography.labelMedium,
                color = gridColor,
            )
            Text(
                "now ${fmtVal(pts.last().second)}$unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                "high ${fmtVal(maxV)}$unit",
                style = MaterialTheme.typography.labelMedium,
                color = gridColor,
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(top = 10.dp)
        ) {
            fun x(t: Long) = (t - from).toFloat() / (now - from).toFloat() * size.width
            fun y(v: Double) = ((hi - v) / (hi - lo)).toFloat() * size.height
            for (i in 0..3) {
                val yy = size.height * i / 3f
                drawLine(
                    gridColor.copy(alpha = .15f),
                    Offset(0f, yy), Offset(size.width, yy),
                    strokeWidth = 1.5f,
                )
            }
            val line = Path()
            pts.forEachIndexed { i, p ->
                if (i == 0) line.moveTo(x(p.first), y(p.second))
                else line.lineTo(x(p.first), y(p.second))
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(x(pts.last().first), size.height)
                lineTo(x(pts.first().first), size.height)
                close()
            }
            drawPath(
                fill,
                Brush.verticalGradient(listOf(color.copy(alpha = .28f), color.copy(alpha = 0f))),
            )
            drawPath(
                line, color,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("24 h ago", "18 h", "12 h", "6 h", "now").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = gridColor)
            }
        }
    }
}
