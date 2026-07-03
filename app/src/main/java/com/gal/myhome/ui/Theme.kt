package com.gal.myhome.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.gal.myhome.data.Accent
import com.gal.myhome.data.Density as UiDensity
import com.gal.myhome.data.Prefs
import com.gal.myhome.data.ThemeMode

private fun accentSchemes(accent: Accent) = when (accent) {
    Accent.AMBER -> lightColorScheme(
        primary = Color(0xFF7A5900), primaryContainer = Color(0xFFFFDF9E),
        onPrimaryContainer = Color(0xFF261A00),
        tertiaryContainer = Color(0xFFFFF2CC), onTertiaryContainer = Color(0xFF4A3A00),
    ) to darkColorScheme(
        primary = Color(0xFFF7CF5A), primaryContainer = Color(0xFF5C4300),
        onPrimaryContainer = Color(0xFFFFDF9E),
        tertiaryContainer = Color(0xFF4A3A12), onTertiaryContainer = Color(0xFFFFE9A8),
    )
    Accent.BLUE -> lightColorScheme(
        primary = Color(0xFF0B57D0), primaryContainer = Color(0xFFD3E3FD),
        onPrimaryContainer = Color(0xFF041E49),
        tertiaryContainer = Color(0xFFD3E3FD), onTertiaryContainer = Color(0xFF041E49),
    ) to darkColorScheme(
        primary = Color(0xFFA8C7FA), primaryContainer = Color(0xFF0842A0),
        onPrimaryContainer = Color(0xFFD3E3FD),
        tertiaryContainer = Color(0xFF1E3A5F), onTertiaryContainer = Color(0xFFD3E3FD),
    )
    Accent.GREEN -> lightColorScheme(
        primary = Color(0xFF146C2E), primaryContainer = Color(0xFFB6F2C0),
        onPrimaryContainer = Color(0xFF002107),
        tertiaryContainer = Color(0xFFD9F2DC), onTertiaryContainer = Color(0xFF0A3818),
    ) to darkColorScheme(
        primary = Color(0xFF9BD8A5), primaryContainer = Color(0xFF0F5223),
        onPrimaryContainer = Color(0xFFB6F2C0),
        tertiaryContainer = Color(0xFF1E4028), onTertiaryContainer = Color(0xFFC2ECC8),
    )
    Accent.PURPLE -> lightColorScheme(
        primary = Color(0xFF6750A4), primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        tertiaryContainer = Color(0xFFEADDFF), onTertiaryContainer = Color(0xFF21005D),
    ) to darkColorScheme(
        primary = Color(0xFFD0BCFF), primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        tertiaryContainer = Color(0xFF3A2E55), onTertiaryContainer = Color(0xFFE6D8FF),
    )
}

@Composable
fun MyHomeTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val dark = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = if (prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val (light, darkScheme) = accentSchemes(prefs.accent)
        if (dark) darkScheme else light
    }

    val fontScale = when (prefs.density) {
        UiDensity.COMPACT -> 0.88f
        UiDensity.DEFAULT -> 1f
        UiDensity.LARGE -> 1.12f
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * fontScale)
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
