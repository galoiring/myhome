package com.gal.myhome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/** Fullscreen on-demand live view (RTSP via Scrypted). Auto-closes after 2 minutes. */
@Composable
fun CameraLiveView(name: String, url: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val context = LocalContext.current
        var status by remember { mutableStateOf("Connecting…") }

        val player = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                playWhenReady = true
                prepare()
            }
        }
        DisposableEffect(Unit) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    status = when (state) {
                        Player.STATE_READY -> ""
                        Player.STATE_BUFFERING -> "Connecting…"
                        Player.STATE_ENDED -> "Stream ended"
                        else -> status
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    status = "Can't play stream: ${error.errorCodeName}"
                }
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
                player.release()
            }
        }
        LaunchedEffect(Unit) {
            delay(2 * 60 * 1000L)
            onClose()
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (status.isNotEmpty()) {
                Box(Modifier.align(Alignment.Center)) {
                    if (status.startsWith("Connecting")) CircularProgressIndicator()
                    else Text(status, color = Color.White,
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text(
                name,
                color = Color.White.copy(alpha = .8f),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(Icons.Rounded.Close, "Close", tint = Color.White)
            }
        }
    }
}
