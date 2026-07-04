package com.gal.myhome.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BlurOn
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import com.gal.myhome.data.Room
import com.gal.myhome.data.TileHeight
import com.gal.myhome.data.TileWidth
import com.gal.myhome.data.Weather
import com.gal.myhome.data.wmoInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

private fun tintFor(kind: TileKind): TintSet = when (kind) {
    TileKind.AC, TileKind.PURIFIER, TileKind.FAN -> CoolTint
    TileKind.CURTAIN -> PurpleTint
    else -> AmberTint
}

// a curtain has no on/off state (it's position-based), so unlike lights/climate
// its tint isn't conditional on tile.isOn — it always reads as its own kind
private fun alwaysTinted(kind: TileKind): Boolean = kind == TileKind.CURTAIN

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
            ) { t -> TileCard(t, vm, onOpenCamera = { liveCam = it }) }
        }
        liveCam?.let { cam ->
            CameraLiveView(name = cam.name, url = cam.url, onClose = { liveCam = null })
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
            if (showWeather) WeatherStrip(ui.weather, ui.indoorTemp)
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
private fun WeatherStrip(w: Weather?, indoor: Double?) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeatherStat("⬆", "${w.hi.roundToInt()}°")
                    WeatherStat("⬇", "${w.lo.roundToInt()}°")
                    WeatherStat("💧", "${w.humidity}%")
                    WeatherStat("💨", "${w.wind.roundToInt()} km/h")
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
    }
}

@Composable
private fun WeatherStat(icon: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(icon, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ---------- room-grouped, no-scroll grid ---------- */

private class RoomRow(val label: String?, val tiles: List<TileUi>)

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
                rows.add(RoomRow(labelableRoom?.label, run.items))
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

private fun packRow(tiles: List<TileUi>): List<PackedColumn> {
    val cols = mutableListOf<PackedColumn>()
    var i = 0
    while (i < tiles.size) {
        val t = tiles[i]
        val next = tiles.getOrNull(i + 1)
        if (t.height == TileHeight.HALF && next?.height == TileHeight.HALF && next.width == t.width) {
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
        val rows = groupIntoRows(tiles)
        val packedRows = rows.map { it.label to packRow(it.tiles) }
        // a shared baseline unit width, sized so the most-constrained row
        // exactly fills the available width
        val minUnitWidth = packedRows.minOf { (_, cols) ->
            val totalUnits = cols.sumOf { it.widthUnits }
            (maxWidth - gap * (cols.size - 1)) / totalUnits
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
            val naturalWidth = (maxWidth - gap * (cols.size - 1)) / totalUnits
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
fun TileCard(tile: TileUi, vm: DashboardViewModel, onOpenCamera: (CameraCfg) -> Unit = {}) {
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
    val nameColor = if (tinted) onContent else MaterialTheme.colorScheme.onSurface
    val subColor = if (tinted) onContent.copy(alpha = .75f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    val tappable = tile.canToggle || tile.camera != null
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
            tile.camera?.let { onOpenCamera(it) } ?: vm.toggleTile(tile)
        },
        enabled = tappable,
        interactionSource = interaction,
        shape = RoundedCornerShape(28.dp),
        color = bg,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        // soft glow wash for an active tile, clipped to the card's own rounded
        // shape by Surface — no RenderEffect/blur() needed, works on minSdk 26
        if (tinted) {
            Box(Modifier.fillMaxSize().background(glowBrush))
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
                TileHead(tile, nameColor, subColor, big = false)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val temp = tile.sensors.firstOrNull { it.kind == SensorKind.TEMP }
                    if (temp != null) {
                        // a Small-width tile doesn't have room for the bigger scale
                        val heroStyle = if (tile.width == TileWidth.SMALL)
                            MaterialTheme.typography.displaySmall
                        else MaterialTheme.typography.displayMedium
                        Text(
                            "${temp.value}°",
                            style = heroStyle,
                            fontWeight = FontWeight.Medium,
                            color = nameColor,
                        )
                    }
                    val rest = tile.sensors.filterNot { it === temp }
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
                }
            } else {
                TileHead(
                    tile, nameColor, subColor, big = false,
                    modeControl = tile.modeControl,
                    onSelectMode = { v -> tile.modeControl?.let { vm.sendChars(it.targets, v) } },
                )
                val soloStepper = tile.controls.count { it is StepCtl } == 1
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    tile.controls.forEach { ControlView(it, vm, nameColor, subColor, soloStepper) }
                    if (tile.sensors.isNotEmpty()) SensorsRow(tile.sensors, nameColor, subColor)
                    if (tile.chips.isNotEmpty()) ChipsRow(tile.chips, vm)
                }
            }
        }
    }
}

@Composable
private fun TileHead(
    tile: TileUi, nameColor: Color, subColor: Color, big: Boolean,
    modeControl: SegCtl? = null, onSelectMode: (Int) -> Unit = {},
) {
    val tint = tintFor(tile.kind)
    val tinted = tile.isOn || alwaysTinted(tile.kind)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (big) 12.dp else 10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (tinted) tint.iconCircle
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(if (big) 50.dp else 40.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    tileIcon(tile.kind), null,
                    Modifier.size(if (big) 26.dp else 21.dp),
                    tint = if (tinted) tint.iconTint else nameColor,
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
) {
    when (ctl) {
        is SliderCtl -> SliderRow(ctl, vm, onColor, subColor)
        is SegCtl -> SegRow(ctl, vm)
        is StepCtl -> StepperRow(ctl, vm, onColor, subColor, big = soloStepper)
        is CurtainCtl -> CurtainRow(ctl, vm, onColor, subColor)
    }
}

@Composable
private fun SliderRow(ctl: SliderCtl, vm: DashboardViewModel, onColor: Color, subColor: Color) {
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
                        if (ctl.warm) Brush.horizontalGradient(
                            listOf(Color(0xFFBCD9FF), Color(0xFFFFF3DA), Color(0xFFFFB84D))
                        ) else Brush.horizontalGradient(listOf(subColor.copy(alpha = 0.22f), subColor.copy(alpha = 0.22f)))
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
private fun SegRow(ctl: SegCtl, vm: DashboardViewModel) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(40.dp)) {
        ctl.options.forEachIndexed { i, (v, label) ->
            SegmentedButton(
                selected = ctl.value == v,
                onClick = { vm.sendChars(ctl.targets, v) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = ctl.options.size),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.surfaceBright,
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
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
    var drag by remember(ctl.id) { mutableStateOf<Float?>(null) }
    val open = (drag ?: ctl.value).coerceIn(0f, 100f)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (open <= 1f) "Closed" else "${open.roundToInt()}% open",
                style = MaterialTheme.typography.titleSmall,
                color = onColor,
            )
            Spacer(Modifier.weight(1f))
            Text("drag to move", style = MaterialTheme.typography.labelSmall, color = subColor)
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
                .height(80.dp)
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

            // curtain fabric — pinned left, recedes rightward as it opens
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (1f - open / 100f).coerceIn(0.03f, 1f))
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
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                // grip on the leading edge
                Box(
                    Modifier
                        .padding(end = 3.dp)
                        .width(4.dp)
                        .fillMaxHeight(0.6f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(gripColor)
                )
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
