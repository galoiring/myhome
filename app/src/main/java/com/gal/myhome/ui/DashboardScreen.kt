package com.gal.myhome.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.BlurOn
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
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.gal.myhome.DashboardViewModel
import com.gal.myhome.SegCtl
import com.gal.myhome.SensorKind
import com.gal.myhome.SensorUi
import com.gal.myhome.SliderCtl
import com.gal.myhome.StepCtl
import com.gal.myhome.SwatchCtl
import com.gal.myhome.TileKind
import com.gal.myhome.TileUi
import com.gal.myhome.data.ClockFormat
import com.gal.myhome.data.Weather
import com.gal.myhome.data.wmoInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.roundToInt

/* ---------- swatch presets (hue, saturation, preview color) ---------- */
private data class Swatch(val h: Int, val s: Int, val color: Color)
private val SWATCHES = listOf(
    Swatch(0, 0, Color(0xFFFFF8E7)), Swatch(35, 60, Color(0xFFFFCF8A)),
    Swatch(0, 85, Color(0xFFFF5252)), Swatch(30, 90, Color(0xFFFF9230)),
    Swatch(60, 80, Color(0xFFFFE44D)), Swatch(120, 75, Color(0xFF5BE65B)),
    Swatch(200, 80, Color(0xFF3EC1FF)), Swatch(270, 75, Color(0xFFB266FF)),
    Swatch(320, 75, Color(0xFFFF5EC4)),
)

/* signature "on" tint — warm amber, independent of the dynamic palette so a
   lit tile always reads as "on" at a glance, matching the web dashboard */
private object OnTint {
    val tileLight = Color(0xFFFFF2CC)
    val tileDark = Color(0xFF4A3A12)
    val contentLight = Color(0xFF4A3A00)
    val contentDark = Color(0xFFFFE9A8)
    val iconCircle = Color(0xFFFFCF47)
    val iconTint = Color(0xFF4A3A00)
}

fun tileIcon(kind: TileKind): ImageVector = when (kind) {
    TileKind.LIGHT -> Icons.Rounded.Lightbulb
    TileKind.AC -> Icons.Rounded.AcUnit
    TileKind.PURIFIER -> Icons.Rounded.Air
    TileKind.FAN -> Icons.Rounded.Cyclone
    TileKind.OUTLET -> Icons.Rounded.Power
    TileKind.SWITCH -> Icons.Rounded.ToggleOn
    TileKind.SENSOR -> Icons.Rounded.Thermostat
    TileKind.CURTAIN -> Icons.Rounded.Curtains
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
            else -> AdaptiveGrid(
                count = tiles.size,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp),
            ) { i -> TileCard(tiles[i], vm) }
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
            Text("My Home", style = MaterialTheme.typography.titleLarge)
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
        if (showClock) {
            Text(
                timeFmt.format(now),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Rounded.Settings, contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

/* ---------- adaptive no-scroll grid ---------- */

@Composable
fun AdaptiveGrid(
    count: Int,
    modifier: Modifier = Modifier,
    tile: @Composable (Int) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val gap = 12.dp
        if (maxHeight > maxWidth) {
            // portrait fallback: normal scrolling grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(240.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(count) { i -> tile(i) }
            }
            return@BoxWithConstraints
        }
        // pick the cols × rows split whose tile shape is closest to ~1.25 w/h
        var bestCols = 1
        var bestScore = Double.NEGATIVE_INFINITY
        for (cols in 1..count) {
            val rows = ceil(count / cols.toFloat()).toInt()
            val tw = (maxWidth - gap * (cols - 1)) / cols
            val th = (maxHeight - gap * (rows - 1)) / rows
            if (tw < 170.dp && cols > 1) continue
            val score = -abs(ln((tw / th) / 1.25))
            if (score > bestScore) {
                bestScore = score
                bestCols = cols
            }
        }
        val cols = bestCols
        val rows = ceil(count / cols.toFloat()).toInt()
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (r in 0 until rows) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    for (c in 0 until cols) {
                        val i = r * cols + c
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (i < count) tile(i)
                        }
                    }
                }
            }
        }
    }
}

/* ---------- tile ---------- */

@Composable
fun TileCard(tile: TileUi, vm: DashboardViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val onTile = if (dark) OnTint.tileDark else OnTint.tileLight
    val onContent = if (dark) OnTint.contentDark else OnTint.contentLight
    val bg by animateColorAsState(
        if (tile.isOn) onTile else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "tileBg",
    )
    val nameColor = if (tile.isOn) onContent else MaterialTheme.colorScheme.onSurface
    val subColor = if (tile.isOn) onContent.copy(alpha = .75f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && tile.canToggle) 0.97f else 1f,
        label = "tilePress",
    )

    // the whole card is the on/off button — inner controls consume their own touches
    Surface(
        onClick = {
            if (prefs.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.toggleTile(tile)
        },
        enabled = tile.canToggle,
        interactionSource = interaction,
        shape = RoundedCornerShape(22.dp),
        color = bg,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
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
            } else {
                TileHead(tile, nameColor, subColor, big = false)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.Bottom),
                ) {
                    tile.controls.forEach { ControlView(it, vm, nameColor, subColor) }
                    if (tile.sensors.isNotEmpty()) SensorsRow(tile.sensors, nameColor, subColor)
                    if (tile.chips.isNotEmpty()) ChipsRow(tile.chips, vm)
                }
            }
        }
    }
}

@Composable
private fun TileHead(tile: TileUi, nameColor: Color, subColor: Color, big: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (big) 12.dp else 10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (tile.isOn) OnTint.iconCircle
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(if (big) 50.dp else 40.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    tileIcon(tile.kind), null,
                    Modifier.size(if (big) 26.dp else 21.dp),
                    tint = if (tile.isOn) OnTint.iconTint else nameColor,
                )
            }
        }
        Column {
            Text(
                tile.name,
                style = MaterialTheme.typography.titleMedium,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tile.sub.isNotEmpty()) {
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
}

@Composable
private fun ControlView(
    ctl: Control,
    vm: DashboardViewModel,
    onColor: Color,
    subColor: Color,
) {
    when (ctl) {
        is SliderCtl -> SliderRow(ctl, vm, onColor, subColor)
        is SegCtl -> SegRow(ctl, vm)
        is StepCtl -> StepperRow(ctl, vm, onColor, subColor)
        is SwatchCtl -> SwatchRow(ctl, vm)
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
            if (ctl.warm) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFBCD9FF), Color(0xFFFFF3DA), Color(0xFFFFB84D))
                            )
                        )
                )
            }
            Slider(
                value = shown.coerceIn(ctl.min, ctl.max),
                onValueChange = { drag = it },
                onValueChangeFinished = {
                    drag?.let { vm.sendChars(ctl.targets, it.roundToInt()) }
                    drag = null
                },
                valueRange = ctl.min..ctl.max,
                colors = if (ctl.warm) SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ) else SliderDefaults.colors(),
                modifier = Modifier.height(36.dp),
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
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(36.dp)) {
        ctl.options.forEachIndexed { i, (v, label) ->
            SegmentedButton(
                selected = ctl.value == v,
                onClick = { vm.sendChars(ctl.targets, v) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = ctl.options.size),
                icon = {},
                label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun StepperRow(ctl: StepCtl, vm: DashboardViewModel, onColor: Color, subColor: Color) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(ctl: SwatchCtl, vm: DashboardViewModel) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SWATCHES.forEach { sw ->
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(sw.color)
                    .border(1.dp, Color.Black.copy(alpha = .10f), CircleShape)
                    .clickable { vm.setColor(ctl.hue, ctl.sat, sw.h, sw.s) }
            )
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
            FilterChip(
                selected = chip.on,
                onClick = {
                    val v = !chip.on
                    vm.sendChars(chip.targets, if (chip.isActive) (if (v) 1 else 0) else v)
                },
                label = { Text(chip.label, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SensorsRow(sensors: List<SensorUi>, onColor: Color, subColor: Color) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        sensors.forEach { s ->
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
