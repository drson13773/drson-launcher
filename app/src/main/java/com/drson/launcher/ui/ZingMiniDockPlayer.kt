package com.drson.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZingMiniDockPlayer(
    songTitle: String = "Zing MP3 - Đang chờ nhạc...",
    artistName: String = "Chạm để mở Zing",
    isPlaying: Boolean = false,
    currentProgress: Float = 0f, // Từ 0f đến 1f
    volume: Float = 0.5f,        // Từ 0f đến 1f
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPrevClick: () -> Unit = {},
    onSeekChanged: (Float) -> Unit = {},
    onVolumeChanged: (Float) -> Unit = {},
    onOpenZingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable { onOpenZingClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC1A1612)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon trạng thái đĩa nhạc / Play
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD4AF37)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Thông tin bài hát & Thanh tua nhạc nhỏ
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = songTitle,
                    color = Color(0xFFFFF0B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artistName,
                    color = Color(0xFFB89E72),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Thanh trượt tua nhạc thu nhỏ
                Slider(
                    value = currentProgress,
                    onValueChange = onSeekChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD4AF37),
                        activeTrackColor = Color(0xFFD4AF37),
                        inactiveTrackColor = Color(0xFF4A3E32)
                    )
                )
            }

            // Các nút điều khiển: Bài trước, Phát/Dừng, Bài sau
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onPrevClick, modifier = Modifier.size(30.dp)) {
                    Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color(0xFFFFF0B8))
                }
                IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFFD4AF37)
                    )
                }
                IconButton(onClick = onNextClick, modifier = Modifier.size(30.dp)) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next", tint = Color(0xFFFFF0B8))
                }
            }

            // Thanh điều chỉnh âm lượng thu gọn
            Column(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Vol",
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(12.dp)
                )
                Slider(
                    value = volume,
                    onValueChange = onVolumeChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD4AF37),
                        activeTrackColor = Color(0xFFD4AF37),
                        inactiveTrackColor = Color(0xFF4A3E32)
                    )
                )
            }
        }
    }
}
