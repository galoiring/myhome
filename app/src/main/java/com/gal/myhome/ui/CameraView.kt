package com.gal.myhome.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
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
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gal.myhome.DashboardViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** URLs currently held open by a fullscreen [CameraLiveView]. The dashboard
 * tile's periodic snapshot poller checks this so it never opens a second,
 * competing connection to the same rebroadcast stream while the live view
 * is on screen. */
private object LiveViewGuard {
    val activeUrls: MutableSet<String> = java.util.Collections.synchronizedSet(mutableSetOf())
}

/** Many RTSP rebroadcasters (Scrypted included) only speak RTP-over-TCP
 * interleaved mode reliably across NAT/Wi-Fi — the default UDP transport
 * ExoPlayer tries first often black-screens silently on mobile networks. */
private fun rtspMediaSource(url: String) =
    RtspMediaSource.Factory()
        .setForceUseRtpTcp(true)
        .createMediaSource(MediaItem.fromUri(url))

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
                setMediaSource(rtspMediaSource(url))
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
            LiveViewGuard.activeUrls.add(url)
            onDispose {
                LiveViewGuard.activeUrls.remove(url)
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

/**
 * Fullscreen doorbell view built on server-rendered JPEG snapshots. The Ring
 * peephole is a WebRTC stream the player can't decode, so instead of live
 * video we poll the dashboard server's /api/snapshot (ffmpeg grabs one frame
 * inside Scrypted) every few seconds while this is on screen — no connection
 * to the camera when nobody's looking. Auto-closes after 2 minutes.
 */
@Composable
fun CameraSnapshotView(
    name: String,
    rtspUrl: String,
    vm: DashboardViewModel,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var status by remember { mutableStateOf("Connecting…") }
        var refreshTick by remember { mutableStateOf(0) }

        LaunchedEffect(refreshTick) {
            // refresh loop: fetch, show, wait, repeat while the view is open
            while (isActive) {
                try {
                    val bytes = vm.cameraSnapshot(rtspUrl)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        bitmap = bmp
                        status = ""
                    } else if (bitmap == null) status = "No image"
                } catch (_: Exception) {
                    if (bitmap == null) status = "Can't reach camera"
                }
                delay(4000)
            }
        }
        LaunchedEffect(Unit) {
            delay(2 * 60 * 1000L)
            onClose()
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            bitmap?.let {
                Image(
                    it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (status.isNotEmpty()) {
                Box(Modifier.align(Alignment.Center)) {
                    if (status.startsWith("Connecting")) CircularProgressIndicator(color = Color.White)
                    else Text(status, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = .45f),
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(9.dp).background(Color(0xFF34A853), CircleShape))
                    Text(
                        name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            IconButton(
                onClick = { refreshTick++ },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) {
                Icon(Icons.Rounded.Refresh, "Refresh now", tint = Color.White)
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(Icons.Rounded.Close, "Close", tint = Color.White)
            }
        }
    }
}

/**
 * Periodic snapshot for the dashboard tile: connects briefly, grabs one
 * decoded frame, disconnects. Cheaper on the doorbell's battery and the
 * network than holding a live stream open on a wall panel around the clock.
 */
@Composable
fun CameraSnapshotBox(url: String, modifier: Modifier = Modifier, refreshMs: Long = 45_000L) {
    val context = LocalContext.current
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    Box(modifier) {
        // capture surface; sized to match the tile so the decoded frame
        // isn't downscaled, but never drawn itself (we draw the still Image)
        AndroidView(
            factory = { ctx -> TextureView(ctx).also { textureView = it } },
            modifier = Modifier.fillMaxSize(),
        )
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .85f))) {
                Box(Modifier.align(Alignment.Center)) {
                    if (failed) {
                        Icon(Icons.Rounded.Videocam, null, tint = Color.White.copy(alpha = .5f))
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }

    LaunchedEffect(url, textureView) {
        val tv = textureView ?: return@LaunchedEffect
        while (isActive) {
            if (url in LiveViewGuard.activeUrls) {
                delay(2000)
                continue
            }
            var player: ExoPlayer? = null
            try {
                player = ExoPlayer.Builder(context).build().apply {
                    setVideoTextureView(tv)
                    setMediaSource(rtspMediaSource(url))
                    playWhenReady = true
                }
                val gotFrame = withTimeoutOrNull(8000) {
                    suspendCancellableCoroutine<Unit> { cont ->
                        val listener = object : Player.Listener {
                            override fun onRenderedFirstFrame() {
                                if (cont.isActive) cont.resume(Unit)
                            }
                            override fun onPlayerError(error: PlaybackException) {
                                if (cont.isActive) cont.resumeWithException(error)
                            }
                        }
                        player.addListener(listener)
                        player.prepare()
                        cont.invokeOnCancellation { player.removeListener(listener) }
                    }
                }
                if (gotFrame != null) {
                    delay(250) // let a couple frames land past the first keyframe
                    tv.bitmap?.let { bitmap = it }
                    failed = false
                } else {
                    failed = true
                }
            } catch (_: Exception) {
                failed = true
            } finally {
                player?.release()
            }
            delay(refreshMs)
        }
    }
}
