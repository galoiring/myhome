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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.gal.myhome.TileUi
import com.gal.myhome.data.Accent
import com.gal.myhome.data.ClockFormat
import com.gal.myhome.data.Density
import com.gal.myhome.data.SortMode
import com.gal.myhome.data.ThemeMode

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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
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
