package com.gal.myhome

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gal.myhome.ui.DashboardScreen
import com.gal.myhome.ui.MyHomeTheme
import com.gal.myhome.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
private fun App(vm: DashboardViewModel = viewModel()) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(prefs.keepScreenOn) {
        val window = (view.context as ComponentActivity).window
        if (prefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    MyHomeTheme(prefs) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            var showSettings by rememberSaveable { mutableStateOf(false) }
            androidx.compose.foundation.layout.Box(Modifier.safeDrawingPadding()) {
                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(vm, onBack = { showSettings = false })
                } else {
                    DashboardScreen(vm, onOpenSettings = { showSettings = true })
                }
            }
        }
    }
}
