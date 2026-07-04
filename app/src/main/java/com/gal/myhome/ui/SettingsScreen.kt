package com.gal.myhome.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gal.myhome.BuildConfig
import com.gal.myhome.DashboardViewModel
import com.gal.myhome.UpdateState
import com.gal.myhome.TileUi
import com.gal.myhome.data.Accent
import com.gal.myhome.data.CameraCfg
import com.gal.myhome.data.ClockFormat
import com.gal.myhome.data.Density
import com.gal.myhome.data.Room
import com.gal.myhome.data.TileHeight
import com.gal.myhome.data.TileWidth
import com.gal.myhome.data.SortMode
import com.gal.myhome.data.ThemeMode
import com.gal.myhome.data.YeelightCfg
import com.gal.myhome.data.YlFound
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: DashboardViewModel, onBack: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    var renameTile by remember { mutableStateOf<TileUi?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, bottom = 24.dp,
            ),
        ) {
            /* ---- connection ---- */
            item { SectionHeader("Connection") }
            item {
                var url by remember(prefs.serverUrl) { mutableStateOf(prefs.serverUrl) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Dashboard server URL") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { vm.updatePrefs(prefs.copy(serverUrl = url.trim().trimEnd('/'))) },
                        enabled = url.trim().trimEnd('/') != prefs.serverUrl,
                    ) { Icon(Icons.Rounded.Check, "Apply") }
                }
            }
            item {
                LabeledSeg(
                    "Refresh every",
                    listOf(1, 2, 3, 5, 10).map { "${it}s" },
                    listOf(1, 2, 3, 5, 10).indexOf(prefs.pollSeconds).coerceAtLeast(0),
                ) { i -> vm.updatePrefs(prefs.copy(pollSeconds = listOf(1, 2, 3, 5, 10)[i])) }
            }

            /* ---- appearance ---- */
            item { SectionHeader("Appearance") }
            item {
                LabeledSeg(
                    "Theme",
                    listOf("System", "Light", "Dark"),
                    prefs.themeMode.ordinal,
                ) { i -> vm.updatePrefs(prefs.copy(themeMode = ThemeMode.entries[i])) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SwitchRow("Material You colors", "Palette from the device wallpaper", prefs.dynamicColor) {
                        vm.updatePrefs(prefs.copy(dynamicColor = it))
                    }
                }
            }
            if (!prefs.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                item {
                    Row(
                        Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Accent",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val colors = mapOf(
                                Accent.AMBER to Color(0xFFF7CF5A),
                                Accent.BLUE to Color(0xFF4285F4),
                                Accent.GREEN to Color(0xFF34A853),
                                Accent.PURPLE to Color(0xFFA142F4),
                            )
                            colors.forEach { (accent, color) ->
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (prefs.accent == accent) Modifier.border(
                                                3.dp, MaterialTheme.colorScheme.onSurface, CircleShape,
                                            ) else Modifier
                                        )
                                        .clickable { vm.updatePrefs(prefs.copy(accent = accent)) }
                                )
                            }
                        }
                    }
                }
            }
            item {
                LabeledSeg(
                    "Text size",
                    listOf("Compact", "Default", "Large"),
                    prefs.density.ordinal,
                ) { i -> vm.updatePrefs(prefs.copy(density = Density.entries[i])) }
            }
            item {
                LabeledSeg(
                    "Clock",
                    listOf("Auto", "12h", "24h"),
                    prefs.clockFormat.ordinal,
                ) { i -> vm.updatePrefs(prefs.copy(clockFormat = ClockFormat.entries[i])) }
            }
            item {
                SwitchRow("Weather strip", "Show weather in the header", prefs.showWeather) {
                    vm.updatePrefs(prefs.copy(showWeather = it))
                }
            }
            item {
                SwitchRow("Clock", "Show the time in the header", prefs.showClock) {
                    vm.updatePrefs(prefs.copy(showClock = it))
                }
            }
            item {
                LabeledSeg(
                    "Sort tiles",
                    listOf("By type", "By name"),
                    prefs.sortMode.ordinal,
                ) { i -> vm.updatePrefs(prefs.copy(sortMode = SortMode.entries[i])) }
            }

            /* ---- behavior ---- */
            item { SectionHeader("Behavior") }
            item {
                SwitchRow("Keep screen on", "Never sleep while the app is open", prefs.keepScreenOn) {
                    vm.updatePrefs(prefs.copy(keepScreenOn = it))
                }
            }
            item {
                SwitchRow("Fullscreen", "Hide the status and navigation bars", prefs.fullscreen) {
                    vm.updatePrefs(prefs.copy(fullscreen = it))
                }
            }
            item {
                SwitchRow("Haptic feedback", "Vibrate on tile toggle", prefs.hapticFeedback) {
                    vm.updatePrefs(prefs.copy(hapticFeedback = it))
                }
            }
            item {
                SwitchRow(
                    "Night mode",
                    "Black screen on a schedule; tap to wake for a minute",
                    prefs.nightMode,
                ) { vm.updatePrefs(prefs.copy(nightMode = it)) }
            }
            if (prefs.nightMode) {
                item {
                    HourStepperRow("From", prefs.nightStartHour) {
                        vm.updatePrefs(prefs.copy(nightStartHour = it))
                    }
                }
                item {
                    HourStepperRow("Until", prefs.nightEndHour) {
                        vm.updatePrefs(prefs.copy(nightEndHour = it))
                    }
                }
            }

            /* ---- yeelight LAN ---- */
            item { SectionHeader("Yeelight (LAN)") }
            item {
                Text(
                    "Direct LAN control, independent of Homebridge. Enable \"LAN Control\" for the device in the Yeelight app first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            items(prefs.yeelights.size) { i ->
                val cfg = prefs.yeelights[i]
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(cfg.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            cfg.ip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {
                        vm.updatePrefs(prefs.copy(yeelights = prefs.yeelights - cfg))
                    }) { Icon(Icons.Rounded.Delete, "Remove", Modifier.size(19.dp)) }
                }
            }
            item {
                var showAdd by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Yeelight")
                }
                if (showAdd) {
                    AddYeelightDialog(
                        vm = vm,
                        onDismiss = { showAdd = false },
                        onAdd = { cfg ->
                            vm.updatePrefs(prefs.copy(yeelights = prefs.yeelights + cfg))
                            showAdd = false
                        },
                    )
                }
            }

            /* ---- cameras ---- */
            item { SectionHeader("Cameras") }
            item {
                Text(
                    "Live view via an RTSP stream. For the Ring doorbell, copy the RTSP Rebroadcast URL from Scrypted (camera → Settings → Streams).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            items(prefs.cameras.size) { i ->
                val cfg = prefs.cameras[i]
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(cfg.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            cfg.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = {
                        vm.updatePrefs(prefs.copy(cameras = prefs.cameras - cfg))
                    }) { Icon(Icons.Rounded.Delete, "Remove", Modifier.size(19.dp)) }
                }
            }
            item {
                var showAdd by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add camera")
                }
                if (showAdd) {
                    AddCameraDialog(
                        onDismiss = { showAdd = false },
                        onAdd = { cfg ->
                            vm.updatePrefs(prefs.copy(cameras = prefs.cameras + cfg))
                            showAdd = false
                        },
                    )
                }
            }

            /* ---- tiles ---- */
            item { SectionHeader("Tiles") }
            item {
                Text(
                    "Names, visibility and groups are stored on the dashboard server, so the web dashboard stays in sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            items(vm.ui.tiles.size) { i ->
                val tile = vm.ui.tiles[i]
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            tileIcon(tile.kind), null,
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                tile.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            val detail = when {
                                tile.isGroup -> "Group · " + tile.origNames.joinToString(", ")
                                tile.origNames.firstOrNull()?.let { it != tile.name } == true ->
                                    tile.origNames.first()
                                else -> null
                            }
                            if (detail != null) {
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (tile.isGroup) {
                            TextButton(onClick = { vm.ungroup(tile) }) { Text("Ungroup") }
                        }
                        IconButton(onClick = { renameTile = tile }) {
                            Icon(Icons.Rounded.Edit, "Rename", Modifier.size(19.dp))
                        }
                        Switch(
                            checked = !tile.hidden,
                            onCheckedChange = { vm.setTileHidden(tile, !it) },
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, start = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { vm.moveTile(tile.key, -1) },
                            enabled = i > 0,
                            modifier = Modifier.size(32.dp),
                        ) { Icon(Icons.Rounded.ArrowUpward, "Move up", Modifier.size(16.dp)) }
                        IconButton(
                            onClick = { vm.moveTile(tile.key, 1) },
                            enabled = i < vm.ui.tiles.size - 1,
                            modifier = Modifier.size(32.dp),
                        ) { Icon(Icons.Rounded.ArrowDownward, "Move down", Modifier.size(16.dp)) }
                        Spacer(Modifier.width(6.dp))
                        RoomDropdown(
                            current = prefs.roomFor(tile.key),
                            onSelect = { vm.setRoom(tile.key, it) },
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val size = prefs.sizeFor(tile.key)
                        WidthDropdown(
                            current = size.width,
                            onSelect = { vm.setTileWidth(tile.key, it) },
                        )
                        Spacer(Modifier.width(6.dp))
                        FilterChip(
                            selected = size.height == TileHeight.HALF,
                            onClick = {
                                vm.setTileHeight(
                                    tile.key,
                                    if (size.height == TileHeight.HALF) TileHeight.NORMAL else TileHeight.HALF,
                                )
                            },
                            label = { Text("Half height", style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f),
                )
            }
            item {
                OutlinedButton(
                    onClick = { showGroupDialog = true },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Combine tiles into a group")
                }
            }

            /* ---- updates ---- */
            item { SectionHeader("Updates") }
            item {
                var url by remember(prefs.updateCheckUrl) { mutableStateOf(prefs.updateCheckUrl) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Update server URL") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { vm.updatePrefs(prefs.copy(updateCheckUrl = url.trim())) },
                        enabled = url.trim() != prefs.updateCheckUrl,
                    ) { Icon(Icons.Rounded.Check, "Apply") }
                }
            }
            item {
                when (val s = vm.updateState) {
                    is UpdateState.Idle -> Row(
                        Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Up to date (v${BuildConfig.VERSION_NAME})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.checkForUpdate() }) { Text("Check now") }
                    }
                    is UpdateState.Checking -> Text(
                        "Checking for updates…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    is UpdateState.Available -> Column(Modifier.padding(top = 6.dp)) {
                        Text(
                            "Update available: v${s.info.versionName}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (s.info.notes.isNotEmpty()) {
                            Text(
                                s.info.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                            )
                        }
                        Button(onClick = { vm.downloadAndInstallUpdate() }) {
                            Text("Download & Install")
                        }
                    }
                    is UpdateState.Downloading -> Row(
                        Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Downloading update…", style = MaterialTheme.typography.bodyMedium)
                    }
                    is UpdateState.Error -> Row(
                        Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            s.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.checkForUpdate() }) { Text("Retry") }
                    }
                }
            }

            /* ---- about ---- */
            item { SectionHeader("About") }
            item {
                Text(
                    "My Home ${BuildConfig.VERSION_NAME} · native Compose app\nDashboard server: ${prefs.serverUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    renameTile?.let { tile ->
        RenameDialog(
            initial = tile.name,
            onDismiss = { renameTile = null },
            onSave = { vm.renameTile(tile, it); renameTile = null },
        )
    }

    if (showGroupDialog) {
        GroupDialog(
            vm = vm,
            onDismiss = { showGroupDialog = false },
        )
    }
}

@Composable
private fun WidthDropdown(current: TileWidth, onSelect: (TileWidth) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.height(34.dp)) {
            Text("Width: ${current.label}", style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TileWidth.entries.forEach { w ->
                DropdownMenuItem(
                    text = { Text(w.label) },
                    onClick = { onSelect(w); expanded = false },
                    leadingIcon = if (w == current) {
                        { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun RoomDropdown(current: Room?, onSelect: (Room) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.height(34.dp)) {
            Text(current?.label ?: "Assign room", style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Room.entries.forEach { r ->
                DropdownMenuItem(
                    text = { Text(r.label) },
                    onClick = { onSelect(r); expanded = false },
                    leadingIcon = if (r == current) {
                        { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        HorizontalDivider(Modifier.padding(top = 18.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LabeledSeg(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(120.dp),
        )
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            options.forEachIndexed { i, opt ->
                SegmentedButton(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                    icon = {},
                    label = { Text(opt, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun HourStepperRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(112.dp),
        )
        FilledTonalIconButton(
            onClick = { onChange((value + 23) % 24) },
            modifier = Modifier.size(36.dp),
        ) { Icon(Icons.Rounded.Remove, "earlier", Modifier.size(18.dp)) }
        Text(
            "%02d:00".format(value),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(80.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { onChange((value + 1) % 24) },
            modifier = Modifier.size(36.dp),
        ) { Icon(Icons.Rounded.Add, "later", Modifier.size(18.dp)) }
    }
}

@Composable
private fun AddYeelightDialog(
    vm: DashboardViewModel,
    onDismiss: () -> Unit,
    onAdd: (YeelightCfg) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<YlFound>?>(null) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Yeelight") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(4.dp))
                OutlinedTextField(
                    value = ip, onValueChange = { ip = it },
                    label = { Text("IP address") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        searching = true
                        scope.launch {
                            found = vm.discoverYeelights()
                            searching = false
                        }
                    },
                    enabled = !searching,
                ) { Text(if (searching) "Searching…" else "Discover on network") }
                found?.let { list ->
                    if (list.isEmpty()) {
                        Text(
                            "Nothing found — check that LAN Control is enabled in the Yeelight app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        list.forEach { f ->
                            TextButton(onClick = { ip = f.ip }) {
                                Text("${f.ip} (${f.model})")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(YeelightCfg(ip.trim(), name.trim().ifEmpty { "Yeelight" })) },
                enabled = ip.trim().matches(Regex("""\d{1,3}(\.\d{1,3}){3}""")),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddCameraDialog(onDismiss: () -> Unit, onAdd: (CameraCfg) -> Unit) {
    var name by remember { mutableStateOf("Doorbell") }
    var url by remember { mutableStateOf("rtsp://") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add camera") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(4.dp))
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("RTSP URL") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(CameraCfg(name.trim().ifEmpty { "Camera" }, url.trim())) },
                enabled = url.trim().startsWith("rtsp://") && url.trim().length > 10,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GroupDialog(vm: DashboardViewModel, onDismiss: () -> Unit) {
    // only single (non-group, non-shelly) accessories can be combined
    val candidates = vm.ui.tiles.filter { !it.isGroup && it.shelly == null }
    var name by remember { mutableStateOf("") }
    val checked = remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Combine into one tile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(4.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    items(candidates.size) { i ->
                        val t = candidates[i]
                        val orig = t.origNames.firstOrNull() ?: return@items
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    checked.value =
                                        if (orig in checked.value) checked.value - orig
                                        else checked.value + orig
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = orig in checked.value,
                                onCheckedChange = null,
                            )
                            Text(t.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vm.createGroup(name, checked.value.toList())
                    onDismiss()
                },
                enabled = checked.value.size >= 2,
            ) { Text("Combine") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
