package com.gal.myhome.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gal.myhome.DashboardViewModel
import com.gal.myhome.TileUi
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private class SeriesDef(val suffix: String, val label: String, val unit: String, val color: Color)

private val SERIES = listOf(
    SeriesDef("temp", "Temperature", "°C", Color(0xFFE8794A)),
    SeriesDef("humidity", "Humidity", "%", Color(0xFF4A9BD9)),
    SeriesDef("pm25", "PM2.5", " µg/m³", Color(0xFF34A853)),
)

private val RANGES = listOf(24 to "24 h", 48 to "48 h", 168 to "7 d")

/* history chart for a sensor tile — data comes from the Pi's 5-minute
 * samples, kept for 7 days */
@Composable
fun HistorySheet(
    tile: TileUi,
    vm: DashboardViewModel,
    origin: Rect? = null, // the tapped tile's window bounds — popup grows out of it
    onClose: () -> Unit,
) {
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

    var requestDismiss by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = { requestDismiss = true },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Dialog windows pop in with no transition of their own — container
        // transform: once the card is measured, spring it from the tapped
        // tile's rect (scaled + translated to cover it) to its resting place,
        // and collapse it back into the tile on the way out
        var cardRect by remember { mutableStateOf<Rect?>(null) }
        val appear = remember { Animatable(0f) }
        LaunchedEffect(cardRect != null) {
            if (cardRect != null) {
                appear.animateTo(
                    1f,
                    spring(dampingRatio = 0.8f, stiffness = 260f),
                )
            }
        }
        val scope = rememberCoroutineScope()
        var closing by remember { mutableStateOf(false) }
        val dismiss: () -> Unit = {
            if (!closing) {
                closing = true
                scope.launch {
                    appear.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
                    onClose()
                }
            }
        }
        // outside-tap / back arrive via onDismissRequest, outside this scope
        LaunchedEffect(requestDismiss) { if (requestDismiss) dismiss() }
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 3.dp,
            modifier = Modifier
                .width(620.dp)
                .onGloballyPositioned { if (cardRect == null) cardRect = it.boundsInWindow() }
                .graphicsLayer {
                    val c = cardRect
                    val p = appear.value
                    if (c == null || c.width <= 0f || c.height <= 0f) {
                        // not measured yet — stay invisible for the first frame
                        alpha = 0f
                        return@graphicsLayer
                    }
                    if (origin != null) {
                        // both rects are window-space; the app is a fullscreen
                        // panel, so tile window ≈ dialog window closely enough
                        val inv = 1f - p // spring overshoot past 1 gives the pop
                        scaleX = 1f + (origin.width / c.width - 1f) * inv
                        scaleY = 1f + (origin.height / c.height - 1f) * inv
                        translationX = (origin.center.x - c.center.x) * inv
                        translationY = (origin.center.y - c.center.y) * inv
                        // fade in fast so the growing card reads as solid
                        alpha = (p * 3f).coerceIn(0f, 1f)
                    } else {
                        alpha = p.coerceIn(0f, 1f)
                        scaleX = 0.9f + 0.1f * p
                        scaleY = 0.9f + 0.1f * p
                        translationY = (1f - p) * 28.dp.toPx()
                    }
                },
        ) {
            var rangeIdx by remember { mutableIntStateOf(0) }
            // content fades in a beat behind the container so the card reads
            // as a surface growing out of the tile, then filling with detail
            Column(
                Modifier
                    .padding(24.dp)
                    .graphicsLayer {
                        alpha = ((appear.value - 0.3f) / 0.7f).coerceIn(0f, 1f)
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tile.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RANGES.forEachIndexed { i, (_, label) ->
                            FilterChip(
                                selected = i == rangeIdx,
                                onClick = { rangeIdx = i },
                                label = { Text(label) },
                            )
                        }
                    }
                    IconButton(onClick = dismiss) { Icon(Icons.Rounded.Close, "close") }
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
                            HistoryChart(pts, def.color, def.unit, RANGES[rangeIdx].first)
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

// 1/2/2.5/5 × 10ⁿ — the usual "nice" axis steps
private fun niceStep(raw: Double): Double {
    val mag = 10.0.pow(floor(log10(raw)))
    val n = raw / mag
    return mag * when {
        n <= 1.0 -> 1.0
        n <= 2.0 -> 2.0
        n <= 2.5 -> 2.5
        n <= 5.0 -> 5.0
        else -> 10.0
    }
}

// gridline times aligned to the local clock: 6h marks for a day, 12h for
// two, midnights for the week view
private fun timeTicks(from: Long, now: Long, hours: Int, zone: ZoneId): List<Long> {
    val ticks = mutableListOf<Long>()
    if (hours >= 168) {
        var t = Instant.ofEpochMilli(from).atZone(zone)
            .truncatedTo(ChronoUnit.DAYS).plusDays(1)
        while (t.toInstant().toEpochMilli() < now) {
            ticks += t.toInstant().toEpochMilli()
            t = t.plusDays(1)
        }
    } else {
        val stepH = if (hours <= 24) 6 else 12
        var t = Instant.ofEpochMilli(from).atZone(zone).truncatedTo(ChronoUnit.HOURS)
        while (t.hour % stepH != 0 || t.toInstant().toEpochMilli() < from) t = t.plusHours(1)
        while (t.toInstant().toEpochMilli() < now) {
            ticks += t.toInstant().toEpochMilli()
            t = t.plusHours(stepH.toLong())
        }
    }
    return ticks
}

@Composable
private fun HistoryChart(all: List<Pair<Long, Double>>, color: Color, unit: String, hours: Int) {
    val now = remember(all) { System.currentTimeMillis() }
    val from = now - hours * 3600_000L
    val pts = remember(all, hours) { all.filter { it.first >= from }.sortedBy { it.first } }
    if (pts.size < 2) {
        Text(
            "Not enough data in this range yet — check back in a little while.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 40.dp),
        )
        return
    }
    val minV = pts.minOf { it.second }
    val maxV = pts.maxOf { it.second }
    // pad the data range, then snap the chart bounds to whole ticks so the
    // gridlines land on readable values
    val rawSpan = (maxV - minV).coerceAtLeast(1.0)
    val step = niceStep(rawSpan * 1.3 / 3.0)
    var lo = floor((minV - rawSpan * 0.15) / step) * step
    if (minV >= 0 && lo < 0) lo = 0.0
    val hi = ceil((maxV + rawSpan * 0.15) / step) * step
    val yTicks = remember(lo, hi, step) {
        generateSequence(lo) { it + step }.takeWhile { it <= hi + step / 2 }.toList()
    }
    val zone = remember { ZoneId.systemDefault() }
    val xTicks = remember(from, now, hours) { timeTicks(from, now, hours, zone) }
    val clockFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE") }
    val scrubFmt = remember(hours) {
        DateTimeFormatter.ofPattern(if (hours >= 168) "EEE HH:mm" else "HH:mm")
    }

    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleBg = MaterialTheme.colorScheme.inverseSurface
    val bubbleFg = MaterialTheme.colorScheme.inverseOnSurface
    val labelStyle = TextStyle(fontSize = 11.sp)
    val measurer = rememberTextMeasurer()
    var scrub by remember(pts) { mutableStateOf<Int?>(null) }

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
                .height(240.dp)
                .padding(top = 10.dp)
                .pointerInput(pts) {
                    // touch-and-drag scrubbing: snap to the sample nearest the
                    // finger, clear the readout on release
                    fun nearest(xPx: Float): Int {
                        val t = from + (xPx / size.width * (now - from)).toLong()
                        val i = pts.binarySearch { it.first.compareTo(t) }
                        val ins = if (i >= 0) i else -i - 1
                        val cands = listOf(ins - 1, ins).filter { it in pts.indices }
                        return cands.minByOrNull { abs(pts[it].first - t) } ?: 0
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        scrub = nearest(down.position.x)
                        drag(down.id) { change ->
                            change.consume()
                            scrub = nearest(change.position.x)
                        }
                        scrub = null
                    }
                }
        ) {
            val xLabelH = 18.dp.toPx()
            val plotH = size.height - xLabelH
            fun x(t: Long) = (t - from).toFloat() / (now - from).toFloat() * size.width
            fun y(v: Double) = ((hi - v) / (hi - lo)).toFloat() * plotH

            // horizontal gridlines at nice values, label riding on each line
            yTicks.forEach { tick ->
                val yy = y(tick)
                drawLine(
                    gridColor.copy(alpha = .15f),
                    Offset(0f, yy), Offset(size.width, yy),
                    strokeWidth = 1.5f,
                )
                val lbl = measurer.measure(AnnotatedString(fmtVal(tick)), labelStyle)
                drawText(lbl, gridColor.copy(alpha = .75f), Offset(2.dp.toPx(), yy - lbl.size.height - 2.dp.toPx()))
            }
            // vertical time gridlines with clock labels beneath the plot
            xTicks.forEach { tick ->
                val xx = x(tick)
                drawLine(
                    gridColor.copy(alpha = .10f),
                    Offset(xx, 0f), Offset(xx, plotH),
                    strokeWidth = 1.5f,
                )
                val zdt = Instant.ofEpochMilli(tick).atZone(zone)
                val text = if (hours >= 168) dayFmt.format(zdt) else clockFmt.format(zdt)
                val lbl = measurer.measure(AnnotatedString(text), labelStyle)
                val lx = (xx - lbl.size.width / 2f).coerceIn(0f, size.width - lbl.size.width)
                drawText(lbl, gridColor, Offset(lx, plotH + 3.dp.toPx()))
            }

            val line = Path()
            pts.forEachIndexed { i, p ->
                if (i == 0) line.moveTo(x(p.first), y(p.second))
                else line.lineTo(x(p.first), y(p.second))
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(x(pts.last().first), plotH)
                lineTo(x(pts.first().first), plotH)
                close()
            }
            drawPath(
                fill,
                Brush.verticalGradient(
                    listOf(color.copy(alpha = .28f), color.copy(alpha = 0f)),
                    endY = plotH,
                ),
            )
            drawPath(
                line, color,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // scrub readout: hairline + dot on the sample, value/time bubble
            // pinned to the top edge and kept inside the chart
            scrub?.let { idx ->
                val p = pts[idx]
                val px = x(p.first)
                val py = y(p.second)
                drawLine(
                    gridColor.copy(alpha = .5f),
                    Offset(px, 0f), Offset(px, plotH),
                    strokeWidth = 2f,
                )
                drawCircle(color, radius = 5.dp.toPx(), center = Offset(px, py))
                drawCircle(bubbleFg, radius = 2.5.dp.toPx(), center = Offset(px, py))
                val zdt = Instant.ofEpochMilli(p.first).atZone(zone)
                val text = "${fmtVal(p.second)}$unit · ${scrubFmt.format(zdt)}"
                val lbl = measurer.measure(
                    AnnotatedString(text),
                    TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                )
                val padX = 10.dp.toPx()
                val padY = 6.dp.toPx()
                val bw = lbl.size.width + padX * 2
                val bh = lbl.size.height + padY * 2
                val bx = (px - bw / 2).coerceIn(0f, size.width - bw)
                drawRoundRect(
                    bubbleBg,
                    topLeft = Offset(bx, 0f),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bh / 2),
                )
                drawText(lbl, bubbleFg, Offset(bx + padX, padY))
            }
        }
    }
}
