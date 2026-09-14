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
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drson.launcher.R

private val GOLD_ACCENT = Color(0xFFD4AF37)

@Composable
fun NowPlayingBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.35f) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var currentVolume by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8)
    }

    fun sendMediaKey(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        audioManager?.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))

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
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Icon App Nhạc
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

        // 2. Cụm Nút Điều Khiển Nhạc
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = GOLD_ACCENT, modifier = Modifier.size(15.dp))
        }

        Box(
            modifier = Modifier
                .size(34.dp)
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
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = GOLD_ACCENT, modifier = Modifier.size(15.dp))
        }

        // 3. Thanh Trượt Tiến Trình Thời Gian
        Column(
            modifier = Modifier
                .width(130.dp)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Slider(
                value = progress,
                onValueChange = { progress = it },
                colors = SliderDefaults.colors(
                    thumbColor = GOLD_ACCENT,
                    activeTrackColor = GOLD_ACCENT,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("01:25", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                Text("04:10", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
            }
        }

        // 4. Nút & Thanh Điều Chỉnh Âm Lượng
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (showVolumeSlider) GOLD_ACCENT.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f))
                    .clickable { showVolumeSlider = !showVolumeSlider },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentVolume > 0) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    contentDescription = "Volume",
                    tint = GOLD_ACCENT,
                    modifier = Modifier.size(15.dp)
                )
            }

            if (showVolumeSlider) {
                Slider(
                    value = currentVolume.toFloat(),
                    onValueChange = {
                        currentVolume = it.toInt()
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                    },
                    valueRange = 0f..maxVolume.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = GOLD_ACCENT,
                        activeTrackColor = GOLD_ACCENT,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .width(75.dp)
                        .height(16.dp)
                )
            }
        }
    }
}
