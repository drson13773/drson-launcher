package com.drson.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drson.launcher.hardware.DeviceControls
import com.drson.launcher.media.MediaControlRepository
import kotlinx.coroutines.delay

private val GOLD = Color(0xFFC99E5C)
private val GOLD_BRIGHT = Color(0xFFE6C178)

/**
 * Thanh điều khiển nhạc ĐẦY ĐỦ, hiển thị thẳng trong Dock (không cần mở bảng riêng) - ảnh bìa,
 * Previous/Play-Pause/Next, Seekbar, và Volume. Không dùng nhãn chữ, chỉ icon + thanh trượt,
 * tận dụng khoảng trống còn lại của Dock.
 */
@Composable
fun NowPlayingBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val nowPlaying by MediaControlRepository.nowPlaying.collectAsState()

    var seekPosition by remember { mutableStateOf(0f) }
    var isDraggingSeek by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(DeviceControls.getVolumeFraction(context)) }

    LaunchedEffect(Unit) { MediaControlRepository.start(context) }

    LaunchedEffect(nowPlaying?.isPlaying) {
        while (true) {
            if (!isDraggingSeek) seekPosition = MediaControlRepository.getLivePositionMs().toFloat()
            delay(500)
        }
    }

    val playing = nowPlaying

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (playing?.albumArt != null) {
            Image(
                bitmap = playing.albumArt.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GOLD.copy(alpha = 0.22f)),
            )
        }

        Spacer(Modifier.width(10.dp))
        TransportIconButton(icon = TransportIcon.PREVIOUS, size = 32.dp, onClick = { MediaControlRepository.previous() })
        Spacer(Modifier.width(6.dp))
        TransportIconButton(
            icon = if (playing?.isPlaying == true) TransportIcon.PAUSE else TransportIcon.PLAY,
            size = 38.dp,
            highlighted = true,
            onClick = { MediaControlRepository.playPause() },
        )
        Spacer(Modifier.width(6.dp))
        TransportIconButton(icon = TransportIcon.NEXT, size = 32.dp, onClick = { MediaControlRepository.next() })

        Spacer(Modifier.width(14.dp))

        val duration = (playing?.durationMs ?: 0L).coerceAtLeast(1L)
        Slider(
            value = seekPosition.coerceIn(0f, duration.toFloat()),
            onValueChange = { isDraggingSeek = true; seekPosition = it },
            onValueChangeFinished = {
                MediaControlRepository.seekTo(seekPosition.toLong())
                isDraggingSeek = false
            },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = GOLD_BRIGHT,
                activeTrackColor = GOLD,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
            modifier = Modifier.width(200.dp).height(24.dp),
        )

        Spacer(Modifier.width(16.dp))

        VolumeIcon(level = volume)
        Spacer(Modifier.width(6.dp))
        Slider(
            value = volume,
            onValueChange = {
                volume = it
                DeviceControls.setVolumeFraction(context, it)
            },
            colors = SliderDefaults.colors(
                thumbColor = GOLD_BRIGHT,
                activeTrackColor = GOLD,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
            modifier = Modifier.width(120.dp).height(24.dp),
        )
    }
}

private enum class TransportIcon { PREVIOUS, NEXT, PLAY, PAUSE }

@Composable
private fun TransportIconButton(icon: TransportIcon, size: Dp, highlighted: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (highlighted) GOLD else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val glyphColor = if (highlighted) Color.Black else GOLD_BRIGHT
        Canvas(modifier = Modifier.size(size * 0.5f)) {
            val w = this.size.width
            val h = this.size.height
            when (icon) {
                TransportIcon.PLAY -> {
                    val path = Path().apply {
                        moveTo(0f, 0f); lineTo(w, h / 2f); lineTo(0f, h); close()
                    }
                    drawPath(path, color = glyphColor)
                }
                TransportIcon.PAUSE -> {
                    drawRect(color = glyphColor, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.32f, h))
                    drawRect(color = glyphColor, topLeft = Offset(w * 0.62f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.32f, h))
                }
                TransportIcon.NEXT -> {
                    val p1 = Path().apply { moveTo(0f, 0f); lineTo(w * 0.55f, h / 2f); lineTo(0f, h); close() }
                    drawPath(p1, color = glyphColor)
                    drawRect(color = glyphColor, topLeft = Offset(w * 0.72f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.16f, h))
                }
                TransportIcon.PREVIOUS -> {
                    val p1 = Path().apply { moveTo(w, 0f); lineTo(w * 0.45f, h / 2f); lineTo(w, h); close() }
                    drawPath(p1, color = glyphColor)
                    drawRect(color = glyphColor, topLeft = Offset(w * 0.12f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.16f, h))
                }
            }
        }
    }
}

@Composable
private fun VolumeIcon(level: Float) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = this.size.width
        val h = this.size.height
        val bodyPath = Path().apply {
            moveTo(0f, h * 0.35f)
            lineTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.62f, h * 0.08f)
            lineTo(w * 0.62f, h * 0.92f)
            lineTo(w * 0.35f, h * 0.65f)
            lineTo(0f, h * 0.65f)
            close()
        }
        drawPath(bodyPath, color = GOLD_BRIGHT)
        if (level > 0.05f) {
            drawArc(
                color = GOLD_BRIGHT,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(w * 0.68f, h * 0.15f),
                size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.7f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
            )
        }
    }
}
