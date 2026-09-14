package com.drson.launcher.ui

import android.content.Intent
import android.provider.MediaStore
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
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drson.launcher.R
import com.drson.launcher.media.MediaControlRepository

private val GOLD_ACCENT = Color(0xFFD4AF37)

@Composable
fun NowPlayingBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mediaRepo = remember { MediaControlRepository.getInstance(context) }
    val playbackState by mediaRepo.playbackState.collectAsState()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ô vuông bìa nhạc: nếu không có album art thì hiển thị icon app Nhạc
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable {
                    try {
                        val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
            contentAlignment = Alignment.Center
        ) {
            if (playbackState.albumArt != null) {
                Image(
                    bitmap = playbackState.albumArt!!.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.icon_music),
                    contentDescription = "Music App",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
        }

        // Nút Previous
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { mediaRepo.skipToPrevious() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = GOLD_ACCENT,
                modifier = Modifier.size(18.dp)
            )
        }

        // Nút Play / Pause
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(GOLD_ACCENT)
                .clickable {
                    if (playbackState.isPlaying) mediaRepo.pause() else mediaRepo.play()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
            )
        }

        // Nút Next
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { mediaRepo.skipToNext() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = GOLD_ACCENT,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
