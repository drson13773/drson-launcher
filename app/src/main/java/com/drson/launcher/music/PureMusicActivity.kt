package com.drson.launcher.music

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.drson.launcher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class PureMusicActivity : ComponentActivity() {

    private lateinit var hiddenPlayer: YouTubeHiddenPlayer

    private val defaultSongs = listOf(
        SongItem("5GlrA44YoM0", "Lối Nhỏ", "Đen Vâu ft. Phương Anh Đào"),
        SongItem("L3wKzyIN1yk", "Bài Này Chill Phết", "Đen ft. MIN"),
        SongItem("2Zl_OgtbH6o", "Tuyển tập TOP 20 Nhạc Lofi Chill Việt", "Lofi Chill"),
        SongItem("fJ9rUzIMcZQ", "Bohemian Rhapsody", "Queen"),
        SongItem("kXYiU_JCYtU", "Numb", "Linkin Park"),
        SongItem("kJQP7kiw5Fk", "Despacito", "Luis Fonsi")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isPlayingState by mutableStateOf(false)
        var currentTitleState by mutableStateOf("Chọn một bài hát để bắt đầu")
        var currentArtistState by mutableStateOf("Dr. Sơn Music Engine")

        hiddenPlayer = YouTubeHiddenPlayer(this) { isPlaying, title ->
            isPlayingState = isPlaying
            if (title.isNotEmpty()) {
                currentTitleState = title
            }
        }

        setContent {
            val scope = rememberCoroutineScope()
            var searchKeyword by remember { mutableStateOf("") }
            var songList by remember { mutableStateOf(defaultSongs) }
            var isSearching by remember { mutableStateOf(false) }

            val transition = rememberInfiniteTransition(label = "disc")
            val rotationAngle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 10000, easing = LinearEasing)
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0F0E0C), Color(0xFF070705))
                        )
                    )
            ) {
                // WebView 1x1 ẩn dưới góc màn hình chạy IFrame Stream
                AndroidView(
                    factory = {
                        hiddenPlayer.webView.apply {
                            layoutParams = FrameLayout.LayoutParams(1, 1)
                        }
                    },
                    modifier = Modifier.size(1.dp)
                )

                // BỐ CỤC CHIA 2 CỘT NGANG (LANDSCAPE SPLIT)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // CỘT 1 (TRÁI - 38%): PLAYER ĐIỀU KHIỂN & ĐĨA THAN
                    Card(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x99181510)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Header góc trái
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_drson_music),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ĐANG PHÁT",
                                    color = Color(0xFFD4AF37),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Đĩa than Gold xoay tròn
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF221C14))
                                    .rotate(if (isPlayingState) rotationAngle else 0f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD4AF37)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Tên bài hát & Nghệ sĩ
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentTitleState,
                                    color = Color(0xFFFFF0B8),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentArtistState,
                                    color = Color(0xFF8C7D68),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }

                            // Các phím điều khiển cỡ lớn
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { /* Previous song */ }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Prev",
                                        tint = Color(0xFFFFF0B8),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isPlayingState) hiddenPlayer.pause() else hiddenPlayer.play()
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD4AF37))
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.Black,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                IconButton(onClick = { /* Next song */ }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next",
                                        tint = Color(0xFFFFF0B8),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }

                    // CỘT 2 (PHẢI - 62%): TÌM KIẾM & DANH SÁCH BÀI HÁT
                    Column(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                    ) {
                        // Thanh tìm kiếm và nút đóng
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchKeyword,
                                onValueChange = { searchKeyword = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Tìm kiếm bài hát, ca sĩ...", color = Color(0xFF7A705E), fontSize = 13.sp) },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (searchKeyword.isNotEmpty()) {
                                                isSearching = true
                                                scope.launch {
                                                    val res = searchYouTube(searchKeyword)
                                                    if (res.isNotEmpty()) songList = res
                                                    isSearching = false
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = "Tìm", tint = Color(0xFFD4AF37))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color(0xFF332D24),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFFD4AF37)
                                )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Thoát về Launcher",
                                    tint = Color(0xFFFFF0B8),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Danh sách bài hát dạng thẻ ngang
                        if (isSearching) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFD4AF37))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(songList) { song ->
                                    SongHorizontalCard(
                                        song = song,
                                        onClick = {
                                            currentTitleState = song.title
                                            currentArtistState = song.artist
                                            hiddenPlayer.loadAndPlay(song.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun searchYouTube(query: String): List<SongItem> = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = "https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=${URLEncoder.encode(query, "UTF-8")}"
        val request = Request.Builder().url(url).build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val jsonStart = body.indexOf("(")
            val jsonEnd = body.lastIndexOf(")")
            val results = mutableListOf<SongItem>()

            if (jsonStart != -1 && jsonEnd != -1) {
                val jsonArray = JSONObject("{\"data\": " + body.substring(jsonStart + 1, jsonEnd) + "}").getJSONArray("data")
                val queries = jsonArray.getJSONArray(1)
                for (i in 0 until minOf(queries.length(), 10)) {
                    val title = queries.getJSONArray(i).getString(0)
                    results.add(SongItem(URLEncoder.encode(title, "UTF-8"), title, "Gợi ý âm nhạc"))
                }
            }
            if (results.isEmpty()) defaultSongs else results
        } catch (_: Exception) {
            defaultSongs
        }
    }

    override fun onDestroy() {
        try {
            val parent = hiddenPlayer.webView.parent as? ViewGroup
            parent?.removeView(hiddenPlayer.webView)
            hiddenPlayer.webView.destroy()
        } catch (_: Exception) {}
        super.onDestroy()
    }
}

@Composable
fun SongHorizontalCard(
    song: SongItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661F1A14))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2B2317)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color(0xFF8C7D68),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Phát",
                tint = Color(0xFFD4AF37),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
