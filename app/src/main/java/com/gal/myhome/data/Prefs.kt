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

// sensible assignments for the devices this home has today; overridable in Settings
private val DEFAULT_ROOMS = mapOf(
    "g:color-a01f7d|color-a575ef" to Room.LIVING,
    "s:192.168.68.77:1" to Room.LIVING,
    "s:192.168.68.77:2" to Room.LIVING,
    "a:Curtain" to Room.LIVING,
    "a:מזגן AC" to Room.WHOLE_HOME,
    "a:Ceeling light" to Room.BEDROOM,
    "a:ceilb-4dc114" to Room.BABY,
    "a:Temperature and Humidity sensor" to Room.BABY,
)

data class YeelightCfg(val ip: String, val name: String)
data class CameraCfg(val name: String, val url: String)

data class Prefs(
    val serverUrl: String = "http://192.168.68.75:8090",
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
    val rooms: Map<String, Room> = emptyMap(),
    val yeelights: List<YeelightCfg> = emptyList(),
    val cameras: List<CameraCfg> = emptyList(),
    // explicit user-chosen tile order; empty means "use the automatic sort"
    val tileOrder: List<String> = emptyList(),
) {
    fun roomFor(key: String): Room? = rooms[key] ?: DEFAULT_ROOMS[key]
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
        val rooms = stringPreferencesKey("rooms")
        val yeelights = stringPreferencesKey("yeelights")
        val cameras = stringPreferencesKey("cameras")
        val tileOrder = stringPreferencesKey("tile_order")
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

    private fun parseCameras(v: String?): List<CameraCfg> {
        if (v.isNullOrEmpty()) return emptyList()
        return try {
            val a = JSONArray(v)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                CameraCfg(o.optString("name", "Camera"), o.getString("url"))
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
            rooms = parseRooms(p[K.rooms]),
            yeelights = parseYeelights(p[K.yeelights]),
            cameras = parseCameras(p[K.cameras]),
            tileOrder = parseStringList(p[K.tileOrder]),
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
            p[K.rooms] = JSONObject(prefs.rooms.mapValues { it.value.name }).toString()
            p[K.yeelights] = JSONArray(prefs.yeelights.map {
                JSONObject().put("ip", it.ip).put("name", it.name)
            }).toString()
            p[K.cameras] = JSONArray(prefs.cameras.map {
                JSONObject().put("name", it.name).put("url", it.url)
            }).toString()
            p[K.tileOrder] = JSONArray(prefs.tileOrder).toString()
        }
    }
}
