package com.drson.launcher.ui.music

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.viewinterop.AndroidView

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val BG_DARK = Color(0xFF0F0D0A)

data class YouTubeTrack(
    val title: String,
    val artist: String,
    val videoId: String
)

class DrSonMusicActivity : ComponentActivity() {

    private val defaultPlaylist = listOf(
        YouTubeTrack("Tuyển Tập Nhạc Trẻ Remix Hay Nhất", "Dr Sơn Car Audio", "5qap5aO4i9A"),
        YouTubeTrack("Nhạc Trữ Tình Bolero Lái Xe", "Acoustic Car Vibes", "jfKfPfyJRdk"),
        YouTubeTrack("Deep Chill Driving Mix", "Relaxing Road", "7NOSDKb0HlU"),
        YouTubeTrack("EDM Bass Boosted Nonstop", "Drive Bass", "kJQP7kiw5Fk")
    )

    private var currentTrackIndex by mutableIntStateOf(0)
    private var isPlaying by mutableStateOf(true)
    private var currentQuality by mutableStateOf("Auto")
    private var webViewInstance: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DrSonMusicScreen(
                playlist = defaultPlaylist,
                currentIndex = currentTrackIndex,
                isPlaying = isPlaying,
                currentQuality = currentQuality,
                onQualityChange = { qualityCode, qualityLabel ->
                    currentQuality = qualityLabel
                    setVideoQuality(qualityCode)
                },
                onTrackSelect = { index ->
                    currentTrackIndex = index
                    isPlaying = true
                    playVideo(defaultPlaylist[index].videoId)
                },
                onPlayPauseToggle = {
                    if (isPlaying) pauseVideo() else resumeVideo()
                    isPlaying = !isPlaying
                },
                onNextTrack = {
                    currentTrackIndex = (currentTrackIndex + 1) % defaultPlaylist.size
                    isPlaying = true
                    playVideo(defaultPlaylist[currentTrackIndex].videoId)
                },
                onPrevTrack = {
                    currentTrackIndex = if (currentTrackIndex - 1 < 0) defaultPlaylist.size - 1 else currentTrackIndex - 1
                    isPlaying = true
                    playVideo(defaultPlaylist[currentTrackIndex].videoId)
                },
                onBack = { finish() },
                onWebViewReady = { webViewInstance = it }
            )
        }
    }

    private fun playVideo(videoId: String) {
        val htmlData = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { margin: 0; background-color: #000000; display: flex; justify-content: center; align-items: center; height: 100vh; overflow: hidden; }
                    iframe { width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <iframe id="player" src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&enablejsapi=1&controls=1&fs=0&rel=0&iv_load_policy=3" allow="autoplay; encrypted-media"></iframe>
            </body>
            </html>
        """.trimIndent()
        webViewInstance?.loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlData, "text/html", "utf-8", null)
    }

    private fun setVideoQuality(quality: String) {
        // Gửi lệnh JavaScript thay đổi chất lượng stream
        val js = "document.querySelector('iframe').contentWindow.postMessage('{\"event\":\"command\",\"func\":\"setPlaybackQuality\",\"args\":[\"$quality\"]}', '*');"
        webViewInstance?.evaluateJavascript(js, null)
    }

    private fun pauseVideo() {
        webViewInstance?.evaluateJavascript("document.querySelector('iframe').contentWindow.postMessage('{\"event\":\"command\",\"func\":\"pauseVideo\",\"args\":\"\"}', '*');", null)
    }

    private fun resumeVideo() {
        webViewInstance?.evaluateJavascript("document.querySelector('iframe').contentWindow.postMessage('{\"event\":\"command\",\"func\":\"playVideo\",\"args\":\"\"}', '*');", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewInstance?.destroy()
        webViewInstance = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                currentTrackIndex = (currentTrackIndex + 1) % defaultPlaylist.size
                playVideo(defaultPlaylist[currentTrackIndex].videoId)
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                currentTrackIndex = if (currentTrackIndex - 1 < 0) defaultPlaylist.size - 1 else currentTrackIndex - 1
                playVideo(defaultPlaylist[currentTrackIndex].videoId)
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (isPlaying) pauseVideo() else resumeVideo()
                isPlaying = !isPlaying
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DrSonMusicScreen(
    playlist: List<YouTubeTrack>,
    currentIndex: Int,
    isPlaying: Boolean,
    currentQuality: String,
    onQualityChange: (String, String) -> Unit,
    onTrackSelect: (Int) -> Unit,
    onPlayPauseToggle: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onBack: () -> Unit,
    onWebViewReady: (WebView) -> Unit
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    val qualityOptions = listOf(
        "default" to "Tự động (Auto)",
        "hd1080" to "1080p (Full HD)",
        "hd720" to "720p (HD)",
        "large" to "480p",
        "medium" to "360p",
        "small" to "240p / Tiết kiệm 4G"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BG_DARK)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CỘT TRÁI: Màn hình Video YouTube + Thanh điều khiển
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar: Nút Back + Tiêu đề + Nút Chọn độ phân giải
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GOLD_BRIGHT)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Dr Sơn Music (Online)",
                        color = GOLD_BRIGHT,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Menu chọn chất lượng video
                Box {
                    Button(
                        onClick = { showQualityMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF221D17)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.border(1.dp, GOLD_ACCENT.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.HighQuality, contentDescription = null, tint = GOLD_BRIGHT, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(currentQuality, color = GOLD_BRIGHT, fontSize = 11.sp)
                    }

                    DropdownMenu(
                        expanded = showQualityMenu,
                        onDismissRequest = { showQualityMenu = false },
                        modifier = Modifier.background(Color(0xFF1E1A14))
                    ) {
                        qualityOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = if (currentQuality == label) GOLD_BRIGHT else Color.White, fontSize = 12.sp) },
                                onClick = {
                                    onQualityChange(code, label)
                                    showQualityMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // WebView nhúng Video YouTube
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, GOLD_ACCENT.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()
                            onWebViewReady(this)
                            
                            val initialId = playlist[currentIndex].videoId
                            val initialHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <style>
                                        body { margin: 0; background-color: #000000; display: flex; justify-content: center; align-items: center; height: 100vh; overflow: hidden; }
                                        iframe { width: 100%; height: 100%; border: none; }
                                    </style>
                                </head>
                                <body>
                                    <iframe id="player" src="https://www.youtube-nocookie.com/embed/$initialId?autoplay=1&enablejsapi=1&controls=1&fs=0&rel=0&iv_load_policy=3" allow="autoplay; encrypted-media"></iframe>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://www.youtube-nocookie.com", initialHtml, "text/html", "utf-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Cụm nút Play / Pause / Next / Prev
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevTrack, modifier = Modifier.size(46.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = GOLD_ACCENT, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(20.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GOLD_ACCENT)
                        .clickable(onClick = onPlayPauseToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(20.dp))
                IconButton(onClick = onNextTrack, modifier = Modifier.size(46.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = GOLD_ACCENT, modifier = Modifier.size(30.dp))
                }
            }
        }

        // CỘT PHẢI: Danh sách Playlist
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Danh sách phát",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(playlist) { index, track ->
                    TrackPlaylistItem(
                        track = track,
                        isSelected = index == currentIndex,
                        onClick = { onTrackSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackPlaylistItem(
    track: YouTubeTrack,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> GOLD_ACCENT.copy(alpha = 0.25f)
                    isFocused -> Color.White.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .focusable(interactionSource = interactionSource)
            .then(
                if (isFocused) Modifier.border(2.dp, GOLD_BRIGHT, RoundedCornerShape(10.dp))
                else if (isSelected) Modifier.border(1.dp, GOLD_ACCENT, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.MusicNote else Icons.Default.PlayCircleOutline,
            contentDescription = null,
            tint = if (isSelected) GOLD_BRIGHT else Color.Gray,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isSelected) GOLD_BRIGHT else Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
