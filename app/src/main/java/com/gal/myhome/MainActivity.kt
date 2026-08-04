package com.gal.myhome

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    // the time window itself, separate from what each feature does with it:
    // "dark theme at night" and "blank the screen at night" are independent
    // switches, so gating the theme on nightMode made the dark-theme toggle
    // silently do nothing whenever blanking was turned off
    val nightHours = inNightWindow(prefs, now)
    val inNight = prefs.nightMode && nightHours
    val showNight = inNight && (now.time - wokenAt > NIGHT_WAKE_MS)

    // Leaving the night window used to flip the panel to the light theme on its
    // own, so a screen left on all night was bright by morning with nobody
    // there. Hold the dark theme past the end of the window until someone
    // actually touches the panel; the first tap hands control back to the
    // normal theme rules.
    var lastTouchAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var nightEndedAt by rememberSaveable { mutableLongStateOf(0L) }
    var wasNight by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(nightHours) {
        if (wasNight && !nightHours) nightEndedAt = System.currentTimeMillis()
        if (nightHours) nightEndedAt = 0L
        wasNight = nightHours
    }
    val heldDark = nightEndedAt > 0L && lastTouchAt < nightEndedAt

    // Compose normally learns about the system light/dark switch from a
    // configuration change, but this panel sits in the foreground for weeks and
    // was only picking the change up when it was force-stopped and reopened.
    // Re-reading the activity's own configuration on the 10s tick means a
    // missed notification corrects itself within one tick instead of never.
    val context = LocalContext.current
    val systemDark = remember(now) {
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    }

    // The schedule itself lives in AlarmManager (see ScreenSchedule) — a
    // Compose tick cannot fire the morning wake-up, because recomposition stops
    // the moment the screen goes off. All this does is keep the alarms in step
    // with the settings.
    LaunchedEffect(
        prefs.screenOffEnabled, prefs.screenOffHour,
        prefs.screenOnEnabled, prefs.screenOnHour,
    ) {
        ScreenSchedule.apply(
            view.context,
            enabled = prefs.screenOffEnabled && ScreenLock.canLock(view.context),
            offHour = prefs.screenOffHour,
            wakeEnabled = prefs.screenOnEnabled,
            onHour = prefs.screenOnHour,
        )
    }

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

    MyHomeTheme(
        prefs,
        forceDark = (nightHours || heldDark) && prefs.nightDarkTheme,
        systemDark = systemDark,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            // soft ambient color blobs behind the translucent tiles, tinted
            // by the time of day: fresh morning, theme accents midday, golden
            // evening, deep-blue night. Only redraws when the hour bucket or
            // theme changes, not on the ViewModel's 3s polls
            val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
            val (ambTarget1, ambTarget2) = when (hour) {
                in 6..9 -> Color(0xFF7FD6A4) to Color(0xFF9BC7F7)
                in 10..15 -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.tertiary
                in 16..19 -> Color(0xFFFF9E5C) to Color(0xFFFF7E9D)
                else -> Color(0xFF5A6BC8) to Color(0xFF2E7D8F)
            }
            val primary by animateColorAsState(ambTarget1, label = "ambient1")
            val tertiary by animateColorAsState(ambTarget2, label = "ambient2")
            Box(
                Modifier
                    .fillMaxSize()
                    // observe every touch without consuming it, purely to know
                    // whether anyone is actually at the panel (see heldDark)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                lastTouchAt = System.currentTimeMillis()
                            }
                        }
                    }
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
