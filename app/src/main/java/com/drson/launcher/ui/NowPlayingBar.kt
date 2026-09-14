package com.drson.launcher.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
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

    fun sendMediaKey(keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ô chứa Icon App Nhạc - Chạm vào để mở nhanh ứng dụng nghe nhạc
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable {
                    try {
                        val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.zing.mp3")
                            intent?.let { context.startActivity(it) }
                        } catch (_: Exception) {}
                    }
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
                .clickable { sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
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
                .clickable { sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
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
                .clickable { sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) },
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
