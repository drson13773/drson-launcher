package com.drson.launcher.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
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

private val GOLD_ACCENT = Color(0xFFD4AF37)

@Composable
fun NowPlayingBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    fun sendMediaKey(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // 1. Dispatch trực tiếp qua AudioManager
        audioManager?.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))

        // 2. Broadcast nhắm đích đến Zing MP3
        val zingIntentDown = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setPackage("com.zing.mp3")
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
        }
        val zingIntentUp = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setPackage("com.zing.mp3")
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
        }
        context.sendOrderedBroadcast(zingIntentDown, null)
        context.sendOrderedBroadcast(zingIntentUp, null)

        // 3. Broadcast toàn hệ thống
        val globalDown = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
        }
        val globalUp = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
        }
        context.sendOrderedBroadcast(globalDown, null)
        context.sendOrderedBroadcast(globalUp, null)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon App Nhạc: Chạm vào mở Zing MP3 hoặc App nhạc mặc định
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable {
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.zing.mp3")
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
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

        // Nút Previous
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable {
                    sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
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

        // Nút Play / Pause
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GOLD_ACCENT)
                .clickable {
                    sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    isPlaying = !isPlaying
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

        // Nút Next
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable {
                    sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
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
