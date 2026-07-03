package com.gal.myhome.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ClockFormat { SYSTEM, H12, H24 }
enum class Accent { AMBER, BLUE, GREEN, PURPLE }
enum class SortMode { AUTO, NAME }
enum class Density { COMPACT, DEFAULT, LARGE }

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
)

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
        val fontScale = floatPreferencesKey("font_scale") // legacy, unused
    }

    private inline fun <reified E : Enum<E>> parse(v: String?, default: E): E =
        v?.let { s -> enumValues<E>().firstOrNull { it.name == s } } ?: default

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
        }
    }
}
