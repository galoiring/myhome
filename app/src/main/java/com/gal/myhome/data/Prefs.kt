package com.gal.myhome.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ClockFormat { SYSTEM, H12, H24 }
enum class Accent { AMBER, BLUE, GREEN, PURPLE }
enum class SortMode { AUTO, NAME }
enum class Density { COMPACT, DEFAULT, LARGE }

enum class Room(val label: String, val priority: Int) {
    LIVING("Living Room", 0),
    WHOLE_HOME("Whole home", 1),
    BEDROOM("Bedroom", 2),
    BABY("Baby Room", 3),
    OTHER("Other", 4),
}

enum class TileWidth(val units: Int, val label: String) {
    SMALL(1, "S"), MEDIUM(2, "M"), LARGE(3, "L"),
}
enum class TileHeight { NORMAL, HALF }
data class TileSizeCfg(val width: TileWidth = TileWidth.MEDIUM, val height: TileHeight = TileHeight.NORMAL)

// sensible assignments for the devices this home has today; overridable in Settings
private val DEFAULT_ROOMS = mapOf(
    "g:color-a01f7d|color-a575ef" to Room.LIVING,
    "s:192.168.68.77:1" to Room.LIVING,
    "s:192.168.68.77:2" to Room.LIVING,
    "a:Curtain" to Room.LIVING,
    "a:Mi Air Purifier" to Room.LIVING,
    "a:מזגן AC" to Room.WHOLE_HOME,
    "a:Ceeling light" to Room.BEDROOM,
    "a:ceilb-4dc114" to Room.BABY,
    "a:Temperature and Humidity sensor" to Room.BABY,
)

// same idea as DEFAULT_ROOMS: sensible starting sizes, overridable per tile in
// Settings. Kitchen/Dining pair into one stacked column (same width, Half
// height); Curtain pairs with the purifier's controls the same way, so both
// actually render short instead of silently reverting to Normal height (a
// lone unpaired Half-height tile has no matching neighbor to share a column
// with, so it would otherwise just render at full height). Both fit without
// scrolling because DashboardScreen sizes their (few, simple) controls with
// equal-share weights rather than a fixed/intrinsic height.
private val DEFAULT_SIZES = mapOf(
    "a:מזגן AC" to TileSizeCfg(TileWidth.LARGE, TileHeight.NORMAL),
    "s:192.168.68.77:1" to TileSizeCfg(TileWidth.SMALL, TileHeight.HALF),
    "s:192.168.68.77:2" to TileSizeCfg(TileWidth.SMALL, TileHeight.HALF),
    "a:Curtain" to TileSizeCfg(TileWidth.MEDIUM, TileHeight.HALF),
    "a:Mi Air Purifier" to TileSizeCfg(TileWidth.MEDIUM, TileHeight.HALF),
    // sensor tiles are pure readouts — compact by default so a future
    // camera tile has room on their rows
    "a:Mi Air Purifier:aq" to TileSizeCfg(TileWidth.SMALL, TileHeight.NORMAL),
    "a:Temperature and Humidity sensor" to TileSizeCfg(TileWidth.SMALL, TileHeight.NORMAL),
)

data class YeelightCfg(val ip: String, val name: String)

// doorbell mode: the tile never connects on its own (kind to a battery cam)
// and the live view pops up automatically when the bell rings
data class CameraCfg(val name: String, val url: String, val doorbell: Boolean = false)

data class Prefs(
    // placeholder — every install needs to set its own backend host in Settings
    val serverUrl: String = "http://192.168.1.100:8090",
    val pollSeconds: Int = 3,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accent: Accent = Accent.AMBER,
    val density: Density = Density.DEFAULT,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val sortMode: SortMode = SortMode.AUTO,
    val keepScreenOn: Boolean = true,
    val fullscreen: Boolean = false,
    val showWeather: Boolean = true,
    val showClock: Boolean = true,
    val hapticFeedback: Boolean = true,
    val nightMode: Boolean = true,
    val nightStartHour: Int = 23,
    val nightEndHour: Int = 7,
    // during night hours the theme goes dark even if set to Light/System —
    // separately toggleable so the Theme control isn't mysteriously "broken"
    val nightDarkTheme: Boolean = true,
    val rooms: Map<String, Room> = emptyMap(),
    val yeelights: List<YeelightCfg> = emptyList(),
    val cameras: List<CameraCfg> = emptyList(),
    // explicit user-chosen tile order; empty means "use the automatic sort"
    val tileOrder: List<String> = emptyList(),
    val tileSizes: Map<String, TileSizeCfg> = emptyMap(),
    // GitHub-hosted manifest so the in-app updater works for any install, not
    // just ones on the original developer's LAN
    val updateCheckUrl: String = "https://raw.githubusercontent.com/galoiring/myhome/main/update.json",
) {
    fun roomFor(key: String): Room? = rooms[key] ?: DEFAULT_ROOMS[key]
    fun sizeFor(key: String): TileSizeCfg = tileSizes[key] ?: DEFAULT_SIZES[key] ?: TileSizeCfg()
}

private val Context.dataStore by preferencesDataStore(name = "prefs")

class PrefsRepo(private val context: Context) {

    private object K {
        val serverUrl = stringPreferencesKey("server_url")
        val pollSeconds = intPreferencesKey("poll_seconds")
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val accent = stringPreferencesKey("accent")
        val density = stringPreferencesKey("density")
        val clockFormat = stringPreferencesKey("clock_format")
        val sortMode = stringPreferencesKey("sort_mode")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val fullscreen = booleanPreferencesKey("fullscreen")
        val showWeather = booleanPreferencesKey("show_weather")
        val showClock = booleanPreferencesKey("show_clock")
        val hapticFeedback = booleanPreferencesKey("haptic_feedback")
        val nightMode = booleanPreferencesKey("night_mode")
        val nightStartHour = intPreferencesKey("night_start_hour")
        val nightEndHour = intPreferencesKey("night_end_hour")
        val nightDarkTheme = booleanPreferencesKey("night_dark_theme")
        val rooms = stringPreferencesKey("rooms")
        val yeelights = stringPreferencesKey("yeelights")
        val cameras = stringPreferencesKey("cameras")
        val tileOrder = stringPreferencesKey("tile_order")
        val tileSizes = stringPreferencesKey("tile_sizes")
        val updateCheckUrl = stringPreferencesKey("update_check_url")
    }

    private inline fun <reified E : Enum<E>> parse(v: String?, default: E): E =
        v?.let { s -> enumValues<E>().firstOrNull { it.name == s } } ?: default

    private fun parseRooms(v: String?): Map<String, Room> {
        if (v.isNullOrEmpty()) return emptyMap()
        return try {
            val o = JSONObject(v)
            buildMap {
                o.keys().forEach { k ->
                    Room.entries.firstOrNull { it.name == o.getString(k) }?.let { put(k, it) }
                }
            }
        } catch (_: Exception) { emptyMap() }
    }

    private fun parseYeelights(v: String?): List<YeelightCfg> {
        if (v.isNullOrEmpty()) return emptyList()
        return try {
            val a = JSONArray(v)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                YeelightCfg(o.getString("ip"), o.optString("name", "Yeelight"))
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseStringList(v: String?): List<String> {
        if (v.isNullOrEmpty()) return emptyList()
        return try {
            val a = JSONArray(v)
            (0 until a.length()).map { a.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseTileSizes(v: String?): Map<String, TileSizeCfg> {
        if (v.isNullOrEmpty()) return emptyMap()
        return try {
            val o = JSONObject(v)
            buildMap {
                o.keys().forEach { k ->
                    val e = o.getJSONObject(k)
                    val width = TileWidth.entries.firstOrNull { it.name == e.optString("width") }
                        ?: TileWidth.MEDIUM
                    val height = TileHeight.entries.firstOrNull { it.name == e.optString("height") }
                        ?: TileHeight.NORMAL
                    put(k, TileSizeCfg(width, height))
                }
            }
        } catch (_: Exception) { emptyMap() }
    }

    private fun parseCameras(v: String?): List<CameraCfg> {
        if (v.isNullOrEmpty()) return emptyList()
        return try {
            val a = JSONArray(v)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                CameraCfg(
                    o.optString("name", "Camera"),
                    o.getString("url"),
                    o.optBoolean("doorbell", false),
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    val flow: Flow<Prefs> = context.dataStore.data.map { p ->
        Prefs(
            serverUrl = p[K.serverUrl] ?: Prefs().serverUrl,
            pollSeconds = p[K.pollSeconds] ?: 3,
            themeMode = parse(p[K.themeMode], ThemeMode.SYSTEM),
            dynamicColor = p[K.dynamicColor] ?: true,
            accent = parse(p[K.accent], Accent.AMBER),
            density = parse(p[K.density], Density.DEFAULT),
            clockFormat = parse(p[K.clockFormat], ClockFormat.SYSTEM),
            sortMode = parse(p[K.sortMode], SortMode.AUTO),
            keepScreenOn = p[K.keepScreenOn] ?: true,
            fullscreen = p[K.fullscreen] ?: false,
            showWeather = p[K.showWeather] ?: true,
            showClock = p[K.showClock] ?: true,
            hapticFeedback = p[K.hapticFeedback] ?: true,
            nightMode = p[K.nightMode] ?: true,
            nightStartHour = p[K.nightStartHour] ?: 23,
            nightEndHour = p[K.nightEndHour] ?: 7,
            nightDarkTheme = p[K.nightDarkTheme] ?: true,
            rooms = parseRooms(p[K.rooms]),
            yeelights = parseYeelights(p[K.yeelights]),
            cameras = parseCameras(p[K.cameras]),
            tileOrder = parseStringList(p[K.tileOrder]),
            tileSizes = parseTileSizes(p[K.tileSizes]),
            updateCheckUrl = p[K.updateCheckUrl] ?: Prefs().updateCheckUrl,
        )
    }

    suspend fun update(prefs: Prefs) {
        context.dataStore.edit { p ->
            p[K.serverUrl] = prefs.serverUrl
            p[K.pollSeconds] = prefs.pollSeconds
            p[K.themeMode] = prefs.themeMode.name
            p[K.dynamicColor] = prefs.dynamicColor
            p[K.accent] = prefs.accent.name
            p[K.density] = prefs.density.name
            p[K.clockFormat] = prefs.clockFormat.name
            p[K.sortMode] = prefs.sortMode.name
            p[K.keepScreenOn] = prefs.keepScreenOn
            p[K.fullscreen] = prefs.fullscreen
            p[K.showWeather] = prefs.showWeather
            p[K.showClock] = prefs.showClock
            p[K.hapticFeedback] = prefs.hapticFeedback
            p[K.nightMode] = prefs.nightMode
            p[K.nightStartHour] = prefs.nightStartHour
            p[K.nightEndHour] = prefs.nightEndHour
            p[K.nightDarkTheme] = prefs.nightDarkTheme
            p[K.rooms] = JSONObject(prefs.rooms.mapValues { it.value.name }).toString()
            p[K.yeelights] = JSONArray(prefs.yeelights.map {
                JSONObject().put("ip", it.ip).put("name", it.name)
            }).toString()
            p[K.cameras] = JSONArray(prefs.cameras.map {
                JSONObject().put("name", it.name).put("url", it.url)
                    .put("doorbell", it.doorbell)
            }).toString()
            p[K.tileOrder] = JSONArray(prefs.tileOrder).toString()
            p[K.tileSizes] = JSONObject(prefs.tileSizes.mapValues {
                JSONObject().put("width", it.value.width.name).put("height", it.value.height.name)
            }).toString()
            p[K.updateCheckUrl] = prefs.updateCheckUrl
        }
    }
}
