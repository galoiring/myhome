package com.gal.myhome

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gal.myhome.data.Prefs
import com.gal.myhome.ui.DashboardScreen
import com.gal.myhome.ui.MyHomeTheme
import com.gal.myhome.ui.SettingsScreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

private fun inNightWindow(prefs: Prefs, now: Date): Boolean {
    val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
    val start = prefs.nightStartHour
    val end = prefs.nightEndHour
    return if (start <= end) hour in start until end
    else hour >= start || hour < end // window wraps past midnight
}

private const val NIGHT_WAKE_MS = 60_000L

@Composable
private fun App(vm: DashboardViewModel = viewModel()) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val view = LocalView.current

    // 10s tick drives the night window and re-blanking after a wake
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(10_000)
        }
    }
    var wokenAt by rememberSaveable { mutableLongStateOf(0L) }
    val inNight = prefs.nightMode && inNightWindow(prefs, now)
    val showNight = inNight && (now.time - wokenAt > NIGHT_WAKE_MS)

    LaunchedEffect(prefs.keepScreenOn, showNight, inNight) {
        val window = (view.context as ComponentActivity).window
        if (showNight) {
            // near-black backlight; also let the system sleep if it wants to
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply { screenBrightness = 0.01f }
        } else {
            window.attributes = window.attributes.apply {
                // woken during the night window: readable but easy on 3am
                // eyes, instead of jumping straight back to full daylight
                screenBrightness = if (inNight) 0.35f
                else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            if (prefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    LaunchedEffect(prefs.fullscreen) {
        val window = (view.context as ComponentActivity).window
        val controller = WindowInsetsControllerCompat(window, view)
        if (prefs.fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    MyHomeTheme(prefs, forceDark = inNight && prefs.nightDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            // soft ambient color blobs behind the translucent tiles — the
            // ViewModel's poll/clock ticks don't touch these theme colors,
            // so this only redraws on an actual theme change, not every 3s
            val primary = MaterialTheme.colorScheme.primary
            val tertiary = MaterialTheme.colorScheme.tertiary
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(primary.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width * 0.10f, size.height * 0.05f),
                                radius = size.minDimension * 0.85f,
                            )
                        )
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(tertiary.copy(alpha = 0.14f), Color.Transparent),
                                center = Offset(size.width * 0.95f, size.height * 0.9f),
                                radius = size.minDimension * 0.95f,
                            )
                        )
                    }
            ) {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                Box(Modifier.safeDrawingPadding()) {
                    if (showSettings) {
                        BackHandler { showSettings = false }
                        SettingsScreen(vm, onBack = { showSettings = false })
                    } else {
                        DashboardScreen(vm, onOpenSettings = { showSettings = true })
                    }
                }
                if (showNight) {
                    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTapGestures { wokenAt = System.currentTimeMillis() }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            timeFmt.format(now),
                            color = Color.White.copy(alpha = .22f),
                            style = MaterialTheme.typography.displayMedium,
                        )
                    }
                }
            }
        }
    }
}
