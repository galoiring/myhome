package com.gal.myhome.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Curtains
import androidx.compose.material.icons.rounded.Cyclone
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gal.myhome.ChipUi
import com.gal.myhome.Control
import com.gal.myhome.CurtainCtl
import com.gal.myhome.DashboardViewModel
import com.gal.myhome.SegCtl
import com.gal.myhome.SensorKind
import com.gal.myhome.SensorUi
import com.gal.myhome.SliderCtl
import com.gal.myhome.StepCtl
import com.gal.myhome.TileKind
import com.gal.myhome.TileUi
import com.gal.myhome.UpdateState
import com.gal.myhome.YlRef
import com.gal.myhome.data.CameraCfg
import com.gal.myhome.data.ClockFormat
import com.gal.myhome.data.HourForecast
import com.gal.myhome.data.Room
import com.gal.myhome.data.TileHeight
import com.gal.myhome.data.TileWidth
import com.gal.myhome.data.Weather
import com.gal.myhome.data.wmoInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/* signature "on" tints, independent of the dynamic palette so a lit tile
   always reads at a glance: warm amber for lights, cool blue for climate */
private class TintSet(
    val tileLight: Color, val tileDark: Color,
    val contentLight: Color, val contentDark: Color,
    val iconCircle: Color, val iconTint: Color,
)

private val AmberTint = TintSet(
    tileLight = Color(0xFFFFF2CC), tileDark = Color(0xFF4A3A12),
    contentLight = Color(0xFF4A3A00), contentDark = Color(0xFFFFE9A8),
    iconCircle = Color(0xFFFFCF47), iconTint = Color(0xFF4A3A00),
)

private val CoolTint = TintSet(
    tileLight = Color(0xFFD8E9FF), tileDark = Color(0xFF16324A),
    contentLight = Color(0xFF0A3355), contentDark = Color(0xFFBBD9FF),
    iconCircle = Color(0xFF7FB5FF), iconTint = Color(0xFF0A2A47),
)

private val PurpleTint = TintSet(
    tileLight = Color(0xFFEADDFF), tileDark = Color(0xFF3A2E55),
    contentLight = Color(0xFF3A2570), contentDark = Color(0xFFE3D9FF),
    iconCircle = Color(0xFFB79CFF), iconTint = Color(0xFF2E1F52),
)

private val TealTint = TintSet(
    tileLight = Color(0xFFDDF1EA), tileDark = Color(0xFF17332C),
    contentLight = Color(0xFF0E3B31), contentDark = Color(0xFFBFE5D9),
    iconCircle = Color(0xFF7ED4BC), iconTint = Color(0xFF0B3A2E),
)

private fun tintFor(kind: TileKind): TintSet = when (kind) {
    TileKind.AC, TileKind.PURIFIER, TileKind.FAN -> CoolTint
    TileKind.CURTAIN -> PurpleTint
    TileKind.SENSOR -> TealTint
    else -> AmberTint
}

// curtains and sensors have no on/off state (position- and reading-based),
// so unlike lights/climate their tint isn't conditional on tile.isOn —
// they always read as their own kind instead of looking like "off" tiles
private fun alwaysTinted(kind: TileKind): Boolean =
    kind == TileKind.CURTAIN || kind == TileKind.SENSOR

fun tileIcon(kind: TileKind): ImageVector = when (kind) {
    TileKind.LIGHT -> Icons.Rounded.Lightbulb
    TileKind.AC -> Icons.Rounded.AcUnit
    TileKind.PURIFIER -> Icons.Rounded.Air
    TileKind.FAN -> Icons.Rounded.Cyclone
    TileKind.OUTLET -> Icons.Rounded.Power
    TileKind.SWITCH -> Icons.Rounded.ToggleOn
    TileKind.SENSOR -> Icons.Rounded.Thermostat
    TileKind.CURTAIN -> Icons.Rounded.Curtains
    TileKind.CAMERA -> Icons.Rounded.Videocam
    TileKind.OTHER -> Icons.Rounded.Widgets
}

private fun sensorIcon(kind: SensorKind): ImageVector = when (kind) {
    SensorKind.TEMP -> Icons.Rounded.Thermostat
    SensorKind.HUMIDITY -> Icons.Rounded.WaterDrop
    SensorKind.AIR_QUALITY -> Icons.Rounded.Eco
    SensorKind.PM25 -> Icons.Rounded.BlurOn
    SensorKind.OCCUPANCY -> Icons.Rounded.Timer
    SensorKind.MOTION -> Icons.AutoMirrored.Rounded.DirectionsWalk
    SensorKind.FILTER -> Icons.Rounded.FilterAlt
}

private fun trimNum(d: Double): String {
    val r = (d * 10).roundToInt() / 10.0
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}

/* ---------- clock ---------- */

@Composable
private fun rememberNow(): Date {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(10_000)
        }
    }
    return now
}

/* ---------- screen ---------- */

@Composable
fun DashboardScreen(vm: DashboardViewModel, onOpenSettings: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val ui = vm.ui
    val now = rememberNow()

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp)
    ) {
        HeaderRow(
            vm = vm,
            now = now,
            showWeather = prefs.showWeather,
            showClock = prefs.showClock,
            clockFormat = prefs.clockFormat,
            onOpenSettings = onOpenSettings,
        )

        val tiles = ui.tiles.filter { !it.hidden }
        var liveCam by remember { mutableStateOf<CameraCfg?>(null) }
        var historyTile by remember { mutableStateOf<TileUi?>(null) }

        // doorbell press (relayed by the server) pops the live view once per
        // ring; closing it early doesn't reopen for the same press
        val doorbellCam = prefs.cameras.firstOrNull { it.doorbell }
        var handledRing by remember { mutableStateOf(0L) }
        LaunchedEffect(ui.ringAt) {
            if (doorbellCam != null && ui.ringAt > handledRing &&
                System.currentTimeMillis() - ui.ringAt < 45_000L
            ) {
                handledRing = ui.ringAt
                liveCam = doorbellCam
            }
        }
        when {
            !ui.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            tiles.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (ui.offline) "Can't reach the dashboard server" else "No devices",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> RoomGroupedGrid(
                tiles = tiles,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp),
            ) { t ->
                TileCard(
                    t, vm,
                    onOpenCamera = { liveCam = it },
                    onOpenHistory = { historyTile = it },
                )
            }
        }
        liveCam?.let { cam ->
            // doorbell cams are WebRTC (unplayable by the RTSP player) — show
            // server-rendered snapshots; other cams keep the live RTSP view
            if (cam.doorbell) {
                CameraSnapshotView(cam.name, cam.url, vm, onClose = { liveCam = null })
            } else {
                CameraLiveView(name = cam.name, url = cam.url, onClose = { liveCam = null })
            }
        }
        historyTile?.let { t ->
            HistorySheet(t, vm, onClose = { historyTile = null })
        }
    }
}

@Composable
private fun HeaderRow(
    vm: DashboardViewModel,
    now: Date,
    showWeather: Boolean,
    showClock: Boolean,
    clockFormat: ClockFormat,
    onOpenSettings: () -> Unit,
) {
    val ui = vm.ui
    val context = LocalContext.current
    val dateFmt = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val is24 = when (clockFormat) {
        ClockFormat.SYSTEM -> android.text.format.DateFormat.is24HourFormat(context)
        ClockFormat.H12 -> false
        ClockFormat.H24 -> true
    }
    val timeFmt = remember(is24) {
        SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column {
            if (showClock) {
                Text(
                    timeFmt.format(now),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                dateFmt.format(now),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (showWeather) WeatherStrip(ui.weather, ui.indoorTemp, ui.powerW)
        }
        if (ui.offline) {
            Text(
                "offline",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        Box {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Rounded.Settings, contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (vm.updateState is UpdateState.Available) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

@Composable
private fun WeatherStrip(w: Weather?, indoor: Double?, power: Double? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (w != null) {
            val (icon, label) = wmoInfo(w.code)
            Text(icon, fontSize = 30.sp)
            Text(
                "${w.temp.roundToInt()}°",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
            )
            Column {
                Text(
                    "$label · feels like ${w.feels.roundToInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WeatherStat(Icons.Rounded.ArrowUpward, "${w.hi.roundToInt()}°")
                    WeatherStat(Icons.Rounded.ArrowDownward, "${w.lo.roundToInt()}°")
                    WeatherStat(Icons.Rounded.WaterDrop, "${w.humidity}%")
                    // rain chance beats wind speed for "do I close the windows"
                    w.rainToday?.let { WeatherStat(Icons.Rounded.Umbrella, "$it%") }
                }
            }
            if (w.hours.isNotEmpty()) {
                Box(
                    Modifier
                        .height(34.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    w.hours.forEach { h -> HourCell(h) }
                }
            }
        }
        if (indoor != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Rounded.Home, null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${trimNum(indoor)}°",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "inside",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // live metered power from the Shelly devices
        if (power != null && power >= 1) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Rounded.Bolt, null,
                        Modifier.size(18.dp),
                        tint = Color(0xFFF29900),
                    )
                    Text(
                        "${power.roundToInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "W",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherStat(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HourCell(h: HourForecast) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "%02d".format(h.hour),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(wmoInfo(h.code).first, fontSize = 15.sp)
        Text(
            "${h.temp.roundToInt()}°",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
        if (h.rain >= 20) {
            Text(
                "${h.rain}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4A9BD9),
            )
        }
    }
}

/* ---------- room-grouped, no-scroll grid ---------- */

// bigSingleRoom: a room with 3+ tiles that owns its row — its packing stays
// as-authored (no vertical pairing of full tiles), unlike merged small-room
// rows where lone tiles are allowed to stack two-high to fill the space
private class RoomRow(val label: String?, val tiles: List<TileUi>, val bigSingleRoom: Boolean = false)

// consecutive same-room runs (the tile list is already room-priority sorted);
// small rooms (<=2 tiles) are merged onto a shared row instead of getting a
// near-empty row of their own — e.g. Bedroom's single light shares a row
// with Baby Room rather than each claiming a full-width row.
private fun groupIntoRows(tiles: List<TileUi>): List<RoomRow> {
    data class Run(val room: Room?, val items: List<TileUi>)
    val runs = mutableListOf<Run>()
    var i = 0
    while (i < tiles.size) {
        val room = tiles[i].room
        var j = i
        while (j < tiles.size && tiles[j].room == room) j++
        runs.add(Run(room, tiles.subList(i, j)))
        i = j
    }

    val rows = mutableListOf<RoomRow>()
    var bufTiles = mutableListOf<TileUi>()
    var bufRooms = mutableListOf<Room>()
    var bufHasUnlabeled = false
    fun flush() {
        if (bufTiles.isNotEmpty()) {
            val label = if (bufRooms.isEmpty()) null
            else bufRooms.distinct().joinToString(" · ") { it.label }
            rows.add(RoomRow(label, bufTiles))
            bufTiles = mutableListOf()
            bufRooms = mutableListOf()
            bufHasUnlabeled = false
        }
    }
    for (run in runs) {
        val labelableRoom = run.room?.takeIf { it != Room.OTHER }
        when {
            run.items.size >= 3 -> {
                flush()
                rows.add(RoomRow(labelableRoom?.label, run.items, bigSingleRoom = true))
            }
            // unassigned devices never share a row with a named room, so a
            // curated room's row stays exactly that room (or merged rooms)
            labelableRoom == null -> {
                flush()
                bufTiles.addAll(run.items)
                bufHasUnlabeled = true
            }
            else -> {
                if (bufHasUnlabeled) flush()
                bufTiles.addAll(run.items)
                bufRooms.add(labelableRoom)
            }
        }
    }
    flush()
    return rows
}

// a column is one Normal-height tile, or two Half-height tiles of the same
// width stacked together; a lone Half-height tile (no matching neighbor)
// just renders at Normal height instead of leaving dead empty space below it
private class PackedColumn(val widthUnits: Int, val tiles: List<TileUi>)

private fun packRow(tiles: List<TileUi>, allowNormalStack: Boolean = false): List<PackedColumn> {
    val cols = mutableListOf<PackedColumn>()
    var i = 0
    while (i < tiles.size) {
        val t = tiles[i]
        val next = tiles.getOrNull(i + 1)
        // pair two same-width tiles into one two-high column: always for
        // Half+Half, and — in merged small-room rows — also for Normal+Normal,
        // so two lone medium tiles (e.g. a bedroom + baby light) sit one above
        // the other instead of each stretching across a near-empty row
        val bothHalf = t.height == TileHeight.HALF && next?.height == TileHeight.HALF
        val bothNormal = allowNormalStack &&
            t.height == TileHeight.NORMAL && next?.height == TileHeight.NORMAL
        if (next != null && next.width == t.width && (bothHalf || bothNormal)) {
            cols.add(PackedColumn(t.width.units, listOf(t, next)))
            i += 2
        } else {
            cols.add(PackedColumn(t.width.units, listOf(t)))
            i += 1
        }
    }
    return cols
}

@Composable
fun RoomGroupedGrid(
    tiles: List<TileUi>,
    modifier: Modifier = Modifier,
    tileContent: @Composable (TileUi) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val gap = 12.dp
        if (maxHeight > maxWidth) {
            // portrait fallback: normal scrolling grid, no room grouping
            LazyVerticalGrid(
                columns = GridCells.Adaptive(240.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(tiles.size) { i -> tileContent(tiles[i]) }
            }
            return@BoxWithConstraints
        }
        val rawRows = groupIntoRows(tiles)
        // coalesce a sparse trailing row (≤2 tiles) up into the row before it
        // when the combined width still fits — so a single leftover tile never
        // claims its own full-height, stretched, mostly-empty line. The merged
        // row then packs with vertical stacking enabled (see packRow)
        val maxRowUnits = 11
        val rows = mutableListOf<RoomRow>()
        for (r in rawRows) {
            val prev = rows.lastOrNull()
            val prevUnits = prev?.tiles?.sumOf { it.width.units } ?: 0
            val rUnits = r.tiles.sumOf { it.width.units }
            if (prev != null && !prev.bigSingleRoom && r.tiles.size <= 2 &&
                prevUnits + rUnits <= maxRowUnits
            ) {
                val label = listOfNotNull(prev.label, r.label)
                    .flatMap { it.split(" · ") }.distinct().joinToString(" · ")
                    .ifEmpty { null }
                rows[rows.size - 1] = RoomRow(label, prev.tiles + r.tiles)
            } else {
                rows.add(r)
            }
        }
        val packedRows = rows.map { it.label to packRow(it.tiles, allowNormalStack = !it.bigSingleRoom) }
        // a shared baseline unit width, sized so the most-constrained row
        // exactly fills the available width. Gaps must be counted per UNIT
        // boundary (totalUnits - 1), not per column: a Medium tile's width is
        // defined as two units plus the gap between them, so dividing by
        // column gaps only made every multi-unit row overflow the right edge
        // by one gap per extra unit — clipping the rightmost tile's content
        val minUnitWidth = packedRows.minOf { (_, cols) ->
            val totalUnits = cols.sumOf { it.widthUnits }
            (maxWidth - gap * (totalUnits - 1)) / totalUnits
        }
        // a sparse row may stretch a bit past the shared baseline (capped) so
        // it doesn't look like a mostly-empty row, but never all the way to
        // its own natural width — that's what made a 3-tile row look
        // oversized next to a 6-tile one before. Any width still left over
        // is centered rather than left trailing on one side. (Computed here,
        // outside ColumnScope below, since `maxWidth` needs the
        // BoxWithConstraints receiver.)
        val rowUnitWidths = packedRows.map { (_, cols) ->
            val totalUnits = cols.sumOf { it.widthUnits }
            val naturalWidth = (maxWidth - gap * (totalUnits - 1)) / totalUnits
            minOf(naturalWidth, minUnitWidth * 1.3f)
        }
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            packedRows.forEachIndexed { rowIndex, (label, cols) ->
                val rowUnitWidth = rowUnitWidths[rowIndex]
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (label != null) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                        )
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    ) {
                        cols.forEach { col ->
                            val colWidth = rowUnitWidth * col.widthUnits + gap * (col.widthUnits - 1)
                            Box(Modifier.width(colWidth).fillMaxHeight()) {
                                if (col.tiles.size == 2) {
                                    Column(
                                        Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(gap),
                                    ) {
                                        Box(Modifier.weight(1f).fillMaxWidth()) { tileContent(col.tiles[0]) }
                                        Box(Modifier.weight(1f).fillMaxWidth()) { tileContent(col.tiles[1]) }
                                    }
                                } else {
                                    tileContent(col.tiles[0])
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------- tile ---------- */

@Composable
fun TileCard(
    tile: TileUi,
    vm: DashboardViewModel,
    onOpenCamera: (CameraCfg) -> Unit = {},
    onOpenHistory: (TileUi) -> Unit = {},
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val tint = tintFor(tile.kind)
    val tinted = tile.isOn || alwaysTinted(tile.kind)
    val onTile = if (dark) tint.tileDark else tint.tileLight
    val onContent = if (dark) tint.contentDark else tint.contentLight
    // glass translucency — subtle enough that legibility from across the room
    // isn't hurt, just enough to let the ambient background blobs show through
    val glassAlpha = 0.90f
    val bg by animateColorAsState(
        (if (tinted) onTile else MaterialTheme.colorScheme.surfaceContainerLow)
            .copy(alpha = glassAlpha),
        label = "tileBg",
    )
    // hairline edge so the glass cards read as distinct panes instead of flat
    // fills: bright glass highlight on light, faint on dark, and a matching
    // warm/cool edge on tinted tiles — animated so on/off stays smooth
    val borderColor by animateColorAsState(
        when {
            tinted -> onContent.copy(alpha = .15f)
            dark -> Color.White.copy(alpha = .08f)
            else -> Color.White.copy(alpha = .55f)
        },
        label = "tileBorder",
    )
    val nameColor = if (tinted) onContent else MaterialTheme.colorScheme.onSurface
    val subColor = if (tinted) onContent.copy(alpha = .75f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    // sensor tiles open their 24h history instead of toggling
    val opensHistory = tile.kind == TileKind.SENSOR && !tile.canToggle
    val tappable = tile.canToggle || tile.camera != null || opensHistory
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && tappable) 0.97f else 1f,
        label = "tilePress",
    )
    val glowBrush = remember(tint, dark) {
        Brush.radialGradient(listOf(tint.iconCircle.copy(alpha = if (dark) 0.30f else 0.22f), Color.Transparent))
    }

    // the whole card is the on/off button — inner controls consume their own touches
    Surface(
        onClick = {
            if (prefs.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            when {
                tile.camera != null -> onOpenCamera(tile.camera)
                opensHistory -> onOpenHistory(tile)
                else -> vm.toggleTile(tile)
            }
        },
        enabled = tappable,
        interactionSource = interaction,
        shape = RoundedCornerShape(28.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        if (tile.kind == TileKind.CAMERA && tile.camera?.doorbell == true) {
            DoorbellTileBody(tile, vm, nameColor, subColor)
            return@Surface
        }
        if (tile.kind == TileKind.CAMERA) {
            Box(Modifier.fillMaxSize()) {
                tile.camera?.let { cfg -> CameraSnapshotBox(cfg.url, Modifier.fillMaxSize()) }
                Row(
                    Modifier.align(Alignment.TopStart).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(tileIcon(tile.kind), null, Modifier.size(18.dp), tint = Color.White)
                    Text(
                        tile.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    tile.sub,
                    color = Color.White.copy(alpha = .75f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                )
            }
            return@Surface
        }

        // drag anywhere across a light tile to dim it — the fill tracks the
        // finger, tap still toggles; inner controls consume their own touches
        var dimDrag by remember(tile.key) { mutableStateOf<Float?>(null) }
        val dimmer = tile.dimmer
        val commitDim: (Float) -> Unit = { v ->
            if (prefs.hapticFeedback) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            // setting brightness on an off light also turns it on
            if (!tile.isOn) vm.toggleTile(tile)
            val yl = dimmer?.yl
            if (yl != null) vm.setYeelight(yl, v.roundToInt())
            else dimmer?.let { vm.sendChars(it.targets, v.roundToInt()) }
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .then(
                    if (dimmer == null) Modifier
                    else Modifier.pointerInput(tile.key) {
                        detectHorizontalDragGestures(
                            onDragStart = { off ->
                                dimDrag = (off.x / size.width * 100f).coerceIn(1f, 100f)
                            },
                            onHorizontalDrag = { change, _ ->
                                dimDrag = (change.position.x / size.width * 100f)
                                    .coerceIn(1f, 100f)
                            },
                            onDragEnd = {
                                dimDrag?.let(commitDim)
                                dimDrag = null
                            },
                            onDragCancel = { dimDrag = null },
                        )
                    }
                )
        ) {
        // the tile's real rendered height — a Half-configured tile with no
        // stacking partner renders full height, and a Normal one can end up
        // short in a stacked column, so config height can't gate content
        val tileMaxHeight = maxHeight
        // brightness fill — the tile's level at a glance; soft trailing edge
        // so a mid-level fill doesn't slice a tall tile with a hard line
        if (dimmer != null) {
            val frac by animateFloatAsState(
                if (tile.isOn || dimDrag != null) {
                    ((dimDrag ?: dimmer.value) / 100f).coerceIn(0f, 1f)
                } else 0f,
                label = "dimFill",
            )
            if (frac > 0.01f) {
                val fillColor = tint.iconCircle.copy(alpha = if (dark) 0.28f else 0.30f)
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(frac)
                        .background(
                            Brush.horizontalGradient(
                                0f to fillColor, 0.82f to fillColor, 1f to Color.Transparent,
                            )
                        )
                )
            }
        }
        // soft glow wash for an active tile, clipped to the card's own rounded
        // shape by Surface — no RenderEffect/blur() needed, works on minSdk 26
        if (tinted) {
            Box(Modifier.fillMaxSize().background(glowBrush))
        }

        val hasBody =
            tile.controls.isNotEmpty() || tile.chips.isNotEmpty() || tile.sensors.isNotEmpty()
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (!hasBody) {
                // simple tile: center the head vertically, larger icon
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    TileHead(tile, nameColor, subColor, big = true)
                }
            } else if (tile.kind == TileKind.SENSOR && tile.controls.isEmpty()) {
                // room sensor: show the reading large, like a thermometer card
                val temp = tile.sensors.firstOrNull { it.kind == SensorKind.TEMP }
                val pm25 = tile.sensors.firstOrNull { it.kind == SensorKind.PM25 }
                val humidity = tile.sensors.firstOrNull { it.kind == SensorKind.HUMIDITY }
                val pmStatus = pm25?.value?.toDoubleOrNull()?.let { pm25Status(it) }
                val tempVal = temp?.value?.toDoubleOrNull()
                // status-colored icon circle: AQ tiles follow the PM2.5 band,
                // temp tiles get a cool/warm accent at the extremes
                val (accentCircle, accentIcon) = when {
                    pmStatus != null -> pmStatus.second to Color.White
                    tempVal != null && tempVal <= 18 -> CoolTint.iconCircle to CoolTint.iconTint
                    tempVal != null && tempVal >= 26 -> AmberTint.iconCircle to AmberTint.iconTint
                    else -> null to null
                }
                TileHead(
                    tile, nameColor, subColor, big = false,
                    accentCircle = accentCircle, accentIcon = accentIcon,
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // a Small-width tile doesn't have room for the bigger scale
                    val heroStyle = if (tile.width == TileWidth.SMALL)
                        MaterialTheme.typography.displaySmall
                    else MaterialTheme.typography.displayMedium
                    if (temp != null) {
                        Text(
                            "${temp.value}°",
                            style = heroStyle,
                            fontWeight = FontWeight.Medium,
                            color = nameColor,
                        )
                    } else if (pm25 != null) {
                        Row {
                            Text(
                                pm25.value,
                                style = heroStyle,
                                fontWeight = FontWeight.Medium,
                                color = nameColor,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Text(
                                pm25.unit,
                                style = MaterialTheme.typography.titleSmall,
                                color = subColor,
                                modifier = Modifier.alignByBaseline().padding(start = 5.dp),
                            )
                        }
                        pmStatus?.let { (label, color) ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = color.copy(alpha = 0.18f),
                                modifier = Modifier.padding(top = 6.dp),
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = color,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                    // humidity rides under the temp hero as a compact pill
                    val humidityPill = humidity.takeIf { temp != null }
                    if (humidityPill != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = subColor.copy(alpha = 0.14f),
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Rounded.WaterDrop, null, Modifier.size(14.dp), tint = subColor)
                                Text(
                                    "${humidityPill.value}${humidityPill.unit}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = nameColor,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                    val rest = tile.sensors.filterNot {
                        it === temp || it === pm25 || it === humidityPill
                    }
                    if (rest.isNotEmpty()) {
                        Row(
                            Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rest.forEach { s ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(sensorIcon(s.kind), null, Modifier.size(16.dp), tint = subColor)
                                    Text(s.value, style = MaterialTheme.typography.titleMedium, color = nameColor)
                                    if (s.unit.isNotEmpty()) {
                                        Text(s.unit, style = MaterialTheme.typography.labelSmall, color = subColor)
                                    }
                                }
                            }
                        }
                    }
                    // 24h trend under the hero fills the tile's dead space —
                    // gated on the measured slot height (not the configured
                    // size), so it draws whenever there's actually room for it
                    if (tileMaxHeight >= 190.dp) {
                        val suffix = if (temp != null) "temp" else if (pm25 != null) "pm25" else null
                        val series = suffix?.let { sfx ->
                            tile.origNames.firstNotNullOfOrNull { vm.histories["$it|$sfx"] }
                        }
                        val dayAgo = System.currentTimeMillis() - 24 * 3600 * 1000L
                        val pts = series?.filter { it.first >= dayAgo }.orEmpty()
                        if (pts.size >= 2) {
                            Sparkline(
                                pts,
                                color = pmStatus?.second ?: nameColor.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .height(34.dp),
                            )
                        }
                    }
                }
            } else {
                TileHead(
                    tile, nameColor, subColor, big = false,
                    modeControl = tile.modeControl,
                    onSelectMode = { v -> tile.modeControl?.let { vm.sendChars(it.targets, v) } },
                )
                val soloStepper = tile.controls.count { it is StepCtl } == 1
                val dimControls = tile.canToggle && !tile.isOn
                // a handful of simple controls (Curtain's one visual, the
                // purifier's speed+mode, the AC's fan+setpoint, a light's
                // warmth row) get equal-share weighted heights so they fill
                // the tile exactly — no dead space and no scrolling in Half
                // height — with sensors and a chip or two on natural-height
                // bottom rows. Richer tiles keep the scrollable fallback
                val flexible = tile.controls.size <= 3 && tile.chips.size <= 2
                if (flexible) {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        tile.controls.forEach { ctl ->
                            Box(
                                Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                ControlView(ctl, vm, nameColor, subColor, soloStepper, dimControls, moonTile = tile)
                            }
                        }
                        if (tile.sensors.isNotEmpty()) SensorsRow(tile.sensors, nameColor, subColor)
                        if (tile.chips.isNotEmpty()) ChipsRow(tile.chips, vm)
                    }
                } else {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        tile.controls.forEach {
                            ControlView(it, vm, nameColor, subColor, soloStepper, dimControls, moonTile = tile)
                        }
                        if (tile.sensors.isNotEmpty()) SensorsRow(tile.sensors, nameColor, subColor)
                        if (tile.chips.isNotEmpty()) ChipsRow(tile.chips, vm)
                    }
                }
            }
            if (dimmer != null) {
                BrightnessBar(
                    level = dimDrag ?: dimmer.value,
                    tint = tint,
                    subColor = subColor,
                    onPreview = { dimDrag = it },
                    onEnd = {
                        dimDrag?.let(commitDim)
                        dimDrag = null
                    },
                    onSet = commitDim,
                )
            }
        }
        // live % readout while dimming
        dimDrag?.let { v ->
            Surface(
                shape = RoundedCornerShape(50),
                color = tint.iconCircle,
                shadowElevation = 2.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            ) {
                Text(
                    "${v.roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = tint.iconTint,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
        }
    }
}

/* battery-friendly doorbell tile: shows the server's LAST cached frame (from
   the most recent ring or peek) with its age — refreshing it never touches
   the camera, so the Ring's battery only pays for actual rings and taps */
@Composable
private fun DoorbellTileBody(tile: TileUi, vm: DashboardViewModel, nameColor: Color, subColor: Color) {
    var snap by remember(tile.key) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var snapTs by remember(tile.key) { mutableStateOf(0L) }
    var ageText by remember(tile.key) { mutableStateOf("") }
    // keyed on ringAt so a fresh ring re-pulls the new frame right away
    LaunchedEffect(tile.key, vm.ui.ringAt) {
        while (isActive) {
            try {
                vm.cameraSnapshotCached(tile.camera!!.url)?.let { s ->
                    android.graphics.BitmapFactory
                        .decodeByteArray(s.bytes, 0, s.bytes.size)
                        ?.let { snap = it; snapTs = s.ts }
                }
            } catch (_: Exception) { /* server briefly away; retry */ }
            if (snapTs > 0) {
                val mins = (System.currentTimeMillis() - snapTs) / 60_000L
                ageText = when {
                    mins < 1 -> "just now"
                    mins < 60 -> "$mins min ago"
                    else -> "${mins / 60} h ago"
                }
            }
            // right after a ring the popup is still grabbing the new frame —
            // check back quickly until we have something, then relax
            delay(if (snap == null) 15_000 else 60_000)
        }
    }

    val bmp = snap
    if (bmp == null) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            TileHead(tile, nameColor, subColor, big = false)
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Videocam, null,
                    Modifier.size(42.dp),
                    tint = subColor.copy(alpha = .55f),
                )
            }
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
        Image(
            bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // legibility scrim behind the header text
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = .55f), Color.Transparent)
                    )
                )
        )
        Row(
            Modifier.align(Alignment.TopStart).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(tileIcon(tile.kind), null, Modifier.size(18.dp), tint = Color.White)
            Text(
                tile.name,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = .45f),
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        ) {
            Text(
                ageText,
                color = Color.White.copy(alpha = .9f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun TileHead(
    tile: TileUi, nameColor: Color, subColor: Color, big: Boolean,
    modeControl: SegCtl? = null, onSelectMode: (Int) -> Unit = {},
    // sensor tiles color the icon circle by reading (AQ status, hot/cold)
    accentCircle: Color? = null, accentIcon: Color? = null,
) {
    val tint = tintFor(tile.kind)
    val tinted = tile.isOn || alwaysTinted(tile.kind)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (big) 12.dp else 10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = accentCircle ?: if (tinted) tint.iconCircle
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(if (big) 50.dp else 40.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    tileIcon(tile.kind), null,
                    Modifier.size(if (big) 26.dp else 21.dp),
                    tint = accentIcon ?: if (tinted) tint.iconTint else nameColor,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                tile.name,
                style = MaterialTheme.typography.titleMedium,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tile.sub.isNotEmpty()) {
                if (modeControl != null) {
                    // mode changes rarely (seasonal heat/cool) — tucked behind
                    // a tap on the subtitle instead of a permanent segmented row
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            Modifier.clickable { expanded = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                tile.sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = subColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(16.dp), tint = subColor)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            modeControl.options.forEach { (v, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { onSelectMode(v); expanded = false },
                                    leadingIcon = if (v == modeControl.value) {
                                        { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                                    } else null,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        tile.sub,
                        style = if (big) MaterialTheme.typography.bodyMedium
                        else MaterialTheme.typography.bodySmall,
                        color = subColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // room is now shown as a row-level label in RoomGroupedGrid, not per tile
    }
}

@Composable
private fun ControlView(
    ctl: Control,
    vm: DashboardViewModel,
    onColor: Color,
    subColor: Color,
    soloStepper: Boolean = false,
    dim: Boolean = false,
    moonTile: TileUi? = null,
) {
    // controls on an off tile fade out so on/off reads at a glance from
    // across the room — a bold 100% brightness bar on an off light lies
    val c = if (dim) onColor.copy(alpha = .45f) else onColor
    val s = if (dim) subColor.copy(alpha = .45f) else subColor
    when (ctl) {
        // the moon pill (if any) lives in the warmth row's label slot
        is SliderCtl -> if (ctl.warm) WarmthDots(ctl, vm, c, s, moonTile) else SliderRow(ctl, vm, c, s, dim)
        is SegCtl -> SegRow(ctl, vm, c, s)
        is StepCtl -> StepperRow(ctl, vm, c, s, big = soloStepper)
        is CurtainCtl -> CurtainRow(ctl, vm, c, s)
    }
}

/* slim always-visible level bar at the foot of a dimmable tile: makes the
   current brightness unambiguous (the card fill alone reads as "just a
   tinted tile" at 100%) and doubles as a precise drag/tap target */
@Composable
private fun BrightnessBar(
    level: Float,
    tint: TintSet,
    subColor: Color,
    onPreview: (Float) -> Unit,
    onEnd: () -> Unit,
    onSet: (Float) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(subColor.copy(alpha = 0.16f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        onPreview((off.x / size.width * 100f).coerceIn(1f, 100f))
                    },
                    onHorizontalDrag = { change, _ ->
                        onPreview((change.position.x / size.width * 100f).coerceIn(1f, 100f))
                    },
                    onDragEnd = { onEnd() },
                    onDragCancel = { onEnd() },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    onSet((off.x / size.width * 100f).coerceIn(1f, 100f))
                }
            },
    ) {
        val frac = (level / 100f).coerceIn(0f, 1f)
        if (frac > 0.02f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(frac)
                    .clip(RoundedCornerShape(13.dp))
                    .background(tint.iconCircle)
            ) {
                // grab handle at the fill's leading edge
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 5.dp)
                        .width(4.dp)
                        .fillMaxHeight(0.55f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }
        }
        Text(
            "${level.roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (frac > 0.18f) tint.iconTint else subColor,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
        )
    }
}

/* warmth as four fixed color-temperature stops — presets beat a fiddly
   slider on a wall panel. min..max maps cool..warm on both HomeKit (mireds)
   and Yeelight (0-100 warmth) scales, so fractions work for either. */
/* tiny 24h trend line under a sensor hero — min/max-normalized so the shape
   always uses the full height, with a soft gradient fill below the line */
@Composable
private fun Sparkline(points: List<Pair<Long, Double>>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val minV = points.minOf { it.second }
        val maxV = points.maxOf { it.second }
        val span = (maxV - minV).takeIf { it > 0.0001 }
        val t0 = points.first().first
        val tSpan = (points.last().first - t0).coerceAtLeast(1L).toFloat()
        val pad = 3.dp.toPx()
        fun x(ts: Long) = (ts - t0) / tSpan * size.width
        fun y(v: Double): Float {
            // constant series draws as a flat mid-line
            val f = if (span == null) 0.5f else ((v - minV) / span).toFloat()
            return size.height - pad - f * (size.height - 2 * pad)
        }
        val line = Path()
        points.forEachIndexed { i, (ts, v) ->
            if (i == 0) line.moveTo(x(ts), y(v)) else line.lineTo(x(ts), y(v))
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(x(points.last().first), size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(
            listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0f)),
            endY = size.height,
        ))
        drawPath(line, color, style = Stroke(
            width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
        ))
    }
}

@Composable
private fun MoonPill(tile: TileUi, vm: DashboardViewModel, subColor: Color, modifier: Modifier = Modifier) {
    val moonOn = tile.moon?.on == true
    val haptics = LocalHapticFeedback.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    Surface(
        onClick = {
            if (prefs.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.toggleMoon(tile)
        },
        shape = RoundedCornerShape(50),
        color = if (moonOn) Color(0xFF3A3563)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                Icons.Rounded.Bedtime, "Moonlight mode",
                Modifier.size(17.dp),
                tint = if (moonOn) Color(0xFFC9C2FF) else subColor,
            )
        }
    }
}

@Composable
private fun WarmthDots(
    ctl: SliderCtl, vm: DashboardViewModel, onColor: Color, subColor: Color,
    moonTile: TileUi? = null,
) {
    val presets = listOf(0f, 1f / 3f, 2f / 3f, 1f).map { ctl.min + (ctl.max - ctl.min) * it }
    val dotColors = listOf(
        Color(0xFFBCD9FF), Color(0xFFEFF2F7), Color(0xFFFFE9C0), Color(0xFFFFB84D),
    )
    val nearest = presets.minByOrNull { abs(it - ctl.value) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (moonTile?.moon != null) {
            // moonlight matters more than the label on these lights — the
            // pill takes over the label slot (same width keeps rows aligned)
            MoonPill(moonTile, vm, subColor, Modifier.width(58.dp).height(34.dp))
            Spacer(Modifier.width(8.dp))
        } else Text(
            ctl.label,
            style = MaterialTheme.typography.bodySmall,
            color = subColor,
            modifier = Modifier.width(66.dp),
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Row(
            // swallow horizontal drags so a brightness drag wandering over
            // (or starting on) the dots can't fire a preset by mistake —
            // the dots only respond to a clean tap
            modifier = Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures(onHorizontalDrag = { _, _ -> })
            },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presets.forEachIndexed { i, v ->
                val selected = v == nearest
                Box(
                    Modifier
                        .size(if (selected) 34.dp else 28.dp)
                        .clip(CircleShape)
                        .background(dotColors[i])
                        .then(
                            if (selected) Modifier.border(2.5.dp, onColor, CircleShape)
                            else Modifier
                        )
                        .clickable {
                            val send = v.roundToInt()
                            if (ctl.yl != null) vm.setYeelight(ctl.yl, send)
                            else vm.sendChars(ctl.targets, send)
                        }
                )
            }
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun SliderRow(
    ctl: SliderCtl, vm: DashboardViewModel, onColor: Color, subColor: Color,
    dim: Boolean = false,
) {
    var drag by remember(ctl.id) { mutableStateOf<Float?>(null) }
    val shown = drag ?: ctl.value
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            ctl.label,
            style = MaterialTheme.typography.bodySmall,
            color = subColor,
            modifier = Modifier.width(66.dp),
            maxLines = 1,
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            // chunky pill-shaped track, easier to see and grab than the M3
            // default; warm sliders show a static color-temperature gradient,
            // plain sliders show a proper filled-vs-unfilled fraction
            val fraction = ((shown - ctl.min) / (ctl.max - ctl.min).coerceAtLeast(0.0001f))
                .coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (ctl.warm) {
                            val a = if (dim) 0.35f else 1f
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFBCD9FF).copy(alpha = a),
                                    Color(0xFFFFF3DA).copy(alpha = a),
                                    Color(0xFFFFB84D).copy(alpha = a),
                                )
                            )
                        } else Brush.horizontalGradient(listOf(subColor.copy(alpha = 0.22f), subColor.copy(alpha = 0.22f)))
                    )
            )
            if (!ctl.warm) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(12.dp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(onColor)
                )
            }
            Slider(
                value = shown.coerceIn(ctl.min, ctl.max),
                onValueChange = { drag = it },
                onValueChangeFinished = {
                    drag?.let { v ->
                        if (ctl.yl != null) vm.setYeelight(ctl.yl, v.roundToInt())
                        else vm.sendChars(ctl.targets, v.roundToInt())
                    }
                    drag = null
                },
                valueRange = ctl.min..ctl.max,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
                modifier = Modifier.height(40.dp),
            )
        }
        Text(
            "${shown.roundToInt()}${ctl.unit}",
            style = MaterialTheme.typography.titleSmall,
            color = onColor,
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun SegRow(ctl: SegCtl, vm: DashboardViewModel, onColor: Color, subColor: Color) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(40.dp)) {
        ctl.options.forEachIndexed { i, (v, label) ->
            val selected = ctl.value == v
            SegmentedButton(
                selected = selected,
                onClick = { vm.sendChars(ctl.targets, v) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = ctl.options.size),
                icon = { SegmentedButtonDefaults.Icon(selected) },
                border = BorderStroke(1.dp, subColor.copy(alpha = .35f)),
                colors = SegmentedButtonDefaults.colors(
                    // fill with the tile's content color so the selection is
                    // unmistakable on plain and tinted tiles, light or dark —
                    // surfaceBright vanished against a light tile
                    activeContainerColor = onColor,
                    activeContentColor = if (onColor.luminance() > 0.5f) Color(0xFF1C1B1F) else Color.White,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = onColor,
                ),
                label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun StepperRow(
    ctl: StepCtl, vm: DashboardViewModel, onColor: Color, subColor: Color,
    big: Boolean = false,
) {
    if (big) {
        // the only stepper on the card (the common case: fixed Cool or Heat
        // mode) gets a big, unlabeled, centered control instead of a plain row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = { bump(ctl, vm, -1) },
                modifier = Modifier.size(48.dp),
            ) { Icon(Icons.Rounded.Remove, "decrease", Modifier.size(22.dp)) }
            Text(
                "${trimNum(ctl.value)}°",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium,
                color = onColor,
            )
            FilledTonalIconButton(
                onClick = { bump(ctl, vm, +1) },
                modifier = Modifier.size(48.dp),
            ) { Icon(Icons.Rounded.Add, "increase", Modifier.size(22.dp)) }
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            ctl.label,
            style = MaterialTheme.typography.bodySmall,
            color = subColor,
            modifier = Modifier.width(66.dp),
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        FilledTonalIconButton(
            onClick = { bump(ctl, vm, -1) },
            modifier = Modifier.size(36.dp),
        ) { Icon(Icons.Rounded.Remove, "decrease", Modifier.size(18.dp)) }
        Text(
            "${trimNum(ctl.value)}°",
            style = MaterialTheme.typography.titleMedium,
            color = onColor,
            modifier = Modifier.width(56.dp),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { bump(ctl, vm, +1) },
            modifier = Modifier.size(36.dp),
        ) { Icon(Icons.Rounded.Add, "increase", Modifier.size(18.dp)) }
    }
}

private fun bump(ctl: StepCtl, vm: DashboardViewModel, dir: Int) {
    var v = ctl.value + dir * ctl.step
    v = v.coerceIn(ctl.min, ctl.max)
    v = (v * 10).roundToInt() / 10.0
    vm.sendChars(ctl.targets, if (v == v.toLong().toDouble()) v.toLong() else v)
}

/* draggable curtain over a small window scene: the fabric is anchored left
   and its leading edge recedes right→left as it opens (0% = fully covered,
   100% = fully gathered left, window visible), mirroring the physical
   curtain. Tap or drag the window to set the position — the interaction is
   unchanged from before, just layered over a decorative Canvas instead of a
   plain gradient bar. */
@Composable
private fun CurtainRow(ctl: CurtainCtl, vm: DashboardViewModel, onColor: Color, subColor: Color) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var drag by remember(ctl.id) { mutableStateOf<Float?>(null) }
    // poll updates and taps glide to the new position; while a finger is
    // down the raw drag value keeps the fabric 1:1 under it
    val settled by animateFloatAsState(ctl.value.coerceIn(0f, 100f), label = "curtainPos")
    val open = (drag ?: settled).coerceIn(0f, 100f)
    val dragging = drag != null

    // fills whatever height the tile gives it (Half-height tiles included)
    // instead of a fixed size that would force scrolling in a compact tile
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (open <= 1f) "Closed" else "${open.roundToInt()}% open",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (dragging) FontWeight.Bold else null,
                color = onColor,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "drag to move",
                style = MaterialTheme.typography.labelSmall,
                color = subColor,
                maxLines = 1,
            )
        }
        val fabricColors = listOf(
            Color(0xFFB79CFF).copy(alpha = 0.55f),
            Color(0xFF8B5CF6).copy(alpha = 0.72f),
            Color(0xFFD8C7FF).copy(alpha = 0.55f),
        )
        val pleatDark = Color(0xFF4B2E9E).copy(alpha = 0.25f)
        val pleatLight = Color.White.copy(alpha = 0.30f)
        val gripColor = Color(0xFF6D4DFF)
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .pointerInput(ctl.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { off ->
                            drag = ((1f - off.x / size.width) * 100f).coerceIn(0f, 100f)
                        },
                        onHorizontalDrag = { change, _ ->
                            drag = ((1f - change.position.x / size.width) * 100f)
                                .coerceIn(0f, 100f)
                        },
                        onDragEnd = {
                            drag?.let { vm.sendChars(ctl.targets, it.roundToInt()) }
                            if (prefs.hapticFeedback) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            drag = null
                        },
                        onDragCancel = { drag = null },
                    )
                }
                .pointerInput("${ctl.id}:tap") {
                    detectTapGestures { off ->
                        val v = ((1f - off.x / size.width) * 100f).coerceIn(0f, 100f)
                        vm.sendChars(ctl.targets, v.roundToInt())
                    }
                }
        ) {
            // window scene: sky, soft outdoor shapes, frame, top rail
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawRect(Brush.verticalGradient(listOf(Color(0xFFCFE7FF), Color(0xFFF2F8FF))))
                drawCircle(Color(0xFFE3F0D2), radius = w * 0.22f, center = Offset(w * 0.18f, h * 1.02f))
                drawCircle(Color(0xFFD8ECC8), radius = w * 0.20f, center = Offset(w * 0.55f, h * 1.05f))
                drawCircle(Color(0xFFE3F0D2), radius = w * 0.18f, center = Offset(w * 0.85f, h * 1.0f))
                val frame = Color.White.copy(alpha = 0.75f)
                drawRect(frame, topLeft = Offset(w * 0.49f, 0f), size = Size(3f, h))
                drawRect(frame, topLeft = Offset(0f, h * 0.5f), size = Size(w, 3f))
                drawRect(Color(0xFFEDEBF7), topLeft = Offset(0f, 0f), size = Size(w, 8f))
            }

            val fraction = (1f - open / 100f).coerceIn(0.03f, 1f)
            // curtain fabric — pinned left, recedes rightward as it opens;
            // leading edge rounded so the fabric sits visually above the window
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(Brush.horizontalGradient(fabricColors))
                    .drawBehind {
                        var x = 10.dp.toPx()
                        val step = 15.dp.toPx()
                        while (x < size.width - 4.dp.toPx()) {
                            drawLine(
                                pleatDark,
                                Offset(x, 6.dp.toPx()),
                                Offset(x, size.height - 6.dp.toPx()),
                                strokeWidth = 2.5f,
                            )
                            drawLine(
                                pleatLight,
                                Offset(x + step / 2f, 6.dp.toPx()),
                                Offset(x + step / 2f, size.height - 6.dp.toPx()),
                                strokeWidth = 1.5f,
                            )
                            x += step
                        }
                    }
            )
            // grip + edge shadow live outside the clipped fabric so the pill
            // can overhang the leading edge and stay visible fully closed
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction),
            ) {
                // soft shadow the fabric casts onto the window
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 12.dp)
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent)
                            )
                        )
                )
                // pill grip on the leading edge
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 5.dp)
                        .fillMaxHeight(0.5f)
                        .width(11.dp)
                        .shadow(3.dp, RoundedCornerShape(6.dp))
                        .background(gripColor)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 2.dp)
                            .width(2.dp)
                            .fillMaxHeight(0.72f)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = 0.45f))
                    )
                    Column(
                        Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .size(2.5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                }
                if (dragging) {
                    // floating readout that tracks the grip while the finger
                    // may be covering the header text
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = gripColor,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 12.dp, y = 5.dp),
                    ) {
                        Text(
                            "${open.roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsRow(chips: List<ChipUi>, vm: DashboardViewModel) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        chips.forEach { chip ->
            val isMoonlight = chip.yl?.prop == "moon"
            FilterChip(
                selected = chip.on,
                onClick = {
                    val v = !chip.on
                    if (chip.yl != null) vm.setYeelight(chip.yl, v)
                    else vm.sendChars(chip.targets, if (chip.isActive) (if (v) 1 else 0) else v)
                },
                label = { Text(chip.label, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (isMoonlight) {
                    { Icon(Icons.Rounded.Bedtime, null, Modifier.size(16.dp)) }
                } else null,
                colors = if (isMoonlight) FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF3A3563),
                    selectedLabelColor = Color(0xFFE3DFFF),
                    selectedLeadingIconColor = Color(0xFFC9C2FF),
                ) else FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

private fun airQualityPillColor(label: String): Color = when (label) {
    "Excellent", "Good" -> Color(0xFF34A853)
    "Fair" -> Color(0xFFF29900)
    "Inferior", "Poor" -> Color(0xFFD93025)
    else -> Color(0xFF9AA0A6)
}

// WHO-ish PM2.5 bands, same palette as airQualityPillColor
private fun pm25Status(v: Double): Pair<String, Color> = when {
    v <= 12 -> "Good" to Color(0xFF34A853)
    v <= 35 -> "Moderate" to Color(0xFFF29900)
    else -> "Poor" to Color(0xFFD93025)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SensorsRow(sensors: List<SensorUi>, onColor: Color, subColor: Color) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        sensors.forEach { s ->
            if (s.kind == SensorKind.AIR_QUALITY) {
                val pillColor = airQualityPillColor(s.value)
                Surface(shape = RoundedCornerShape(50), color = pillColor.copy(alpha = 0.18f)) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(sensorIcon(s.kind), null, Modifier.size(13.dp), tint = pillColor)
                        Text(
                            "${s.value} Air Quality",
                            style = MaterialTheme.typography.labelSmall,
                            color = pillColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(sensorIcon(s.kind), null, Modifier.size(15.dp), tint = subColor)
                    Text(
                        s.value,
                        style = MaterialTheme.typography.titleSmall,
                        color = onColor,
                    )
                    if (s.unit.isNotEmpty()) {
                        Text(s.unit, style = MaterialTheme.typography.labelSmall, color = subColor)
                    }
                }
            }
        }
    }
}
