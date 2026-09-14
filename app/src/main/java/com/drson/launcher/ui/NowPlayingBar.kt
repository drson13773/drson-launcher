package com.drson.launcher.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.MediaStore
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drson.launcher.R
import com.drson.launcher.notifications.LauncherNotificationListenerService

private val GOLD_ACCENT = Color(0xFFD4AF37)

@Composable
fun NowPlayingBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    fun getActiveMediaController(): MediaController? {
        return try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val notificationListener = ComponentName(context, LauncherNotificationListenerService::class.java)
            val controllers = sessionManager?.getActiveSessions(notificationListener)
            // Tìm session đang phát hoặc session đầu tiên
            controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers?.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun sendMediaCommand(action: (MediaController.TransportControls) -> Unit, fallbackKeyCode: Int) {
        val controller = getActiveMediaController()
        if (controller != null) {
            action(controller.transportControls)
        } else {
            // Fallback gửi qua Broadcast toàn hệ thống
            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, fallbackKeyCode))
            }
            context.sendOrderedBroadcast(downIntent, null)

            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, fallbackKeyCode))
            }
            context.sendOrderedBroadcast(upIntent, null)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ô bìa / Icon App nhạc
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable {
                    try {
                        val controller = getActiveMediaController()
                        if (controller != null && controller.sessionActivity != null) {
                            controller.sessionActivity?.send()
                        } else {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.zing.mp3")
                                ?: Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    } catch (_: Exception) {}
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_music),
                contentDescription = "Music App",
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        }

        // Nút Lùi bài (Previous)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable {
                    sendMediaCommand({ it.skipToPrevious() }, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = GOLD_ACCENT,
                modifier = Modifier.size(16.dp)
            )
        }

        // Nút Phát / Tạm dừng (Play / Pause)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GOLD_ACCENT)
                .clickable {
                    val controller = getActiveMediaController()
                    if (controller != null) {
                        val state = controller.playbackState?.state
                        if (state == PlaybackState.STATE_PLAYING) {
                            controller.transportControls.pause()
                            isPlaying = false
                        } else {
                            controller.transportControls.play()
                            isPlaying = true
                        }
                    } else {
                        sendMediaCommand({ it.play() }, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        isPlaying = !isPlaying
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }

        // Nút Bài kế tiếp (Next)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable {
                    sendMediaCommand({ it.skipToNext() }, KeyEvent.KEYCODE_MEDIA_NEXT)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = GOLD_ACCENT,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
