package com.drson.launcher.ui.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drson.launcher.R

private val GOLD_BRIGHT = Color(0xFFFFF0B8)
private val GOLD_ACCENT = Color(0xFFD4AF37)
private val BG_DARK = Color(0xFF0C0A08)
private val CARD_DARK = Color(0xFF161410)

// Data Models
data class MusicTrack(val id: String, val title: String, val artist: String, val duration: String)
data class MusicAlbum(val id: String, val title: String, val trackCount: String)
data class MusicArtist(val id: String, val name: String, val fans: String)

class DrSonMusicActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrSonMusicScreen(onClose = { finish() })
        }
    }
}

@Composable
fun DrSonMusicScreen(onClose: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var currentTrack by remember { mutableStateOf(MusicTrack("1", "Ghé Qua (Acoustic)", "Dick ft. PC & Tofu", "04:15")) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackProgress by remember { mutableFloatStateOf(0.42f) }

    // Mock Data YouTube Music
    val tracks = remember {
        listOf(
            MusicTrack("1", "Ghé Qua (Acoustic)", "Dick ft. PC & Tofu", "04:15"),
            MusicTrack("2", "Nấu Ăn Cho Em", "Đen Vâu ft. PiaLinh", "04:45"),
            MusicTrack("3", "Bật Tình Yêu Lên", "Tăng Duy Tân x Hòa Minzy", "03:32"),
            MusicTrack("4", "Cắt Đôi Nỗi Sầu (Speed Up)", "Tăng Duy Tân", "03:10"),
            MusicTrack("5", "Ngày Chưa Giông Bão", "Bùi Lan Hương", "04:20"),
            MusicTrack("6", "Chuyện Rằng", "Thịnh Suy", "03:55")
        )
    }

    val albums = remember {
        listOf(
            MusicAlbum("1", "Acoustic Chill Việt Nam", "24 bài hát"),
            MusicAlbum("2", "Top Hits Bolero Nhạc Xe", "30 bài hát"),
            MusicAlbum("3", "EDM Nonstop Bay Bổng", "18 bài hát"),
            MusicAlbum("4", "Đen Vâu Collection", "15 bài hát")
        )
    }

    val artists = remember {
        listOf(
            MusicArtist("1", "Đen Vâu", "2.1M lượt nghe"),
            MusicArtist("2", "Tăng Duy Tân", "1.5M lượt nghe"),
            MusicArtist("3", "Thịnh Suy", "850K lượt nghe"),
            MusicArtist("4", "Vũ.", "1.9M lượt nghe")
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BG_DARK)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP BAR: Branding + Search + Tabs + Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo Dr Sơn Music
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Logo",
                        tint = GOLD_ACCENT,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "DR SƠN MUSIC",
                        color = GOLD_BRIGHT,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3 Tabs: BÀI HÁT / ALBUM / NGHỆ SĨ
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("BÀI HÁT", "ALBUM / PLAYLIST", "NGHỆ SĨ").forEachIndexed { index, tabName ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == index) GOLD_ACCENT.copy(alpha = 0.22f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (selectedTab == index) GOLD_BRIGHT else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tabName,
                                color = if (selectedTab == index) GOLD_BRIGHT else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm nhạc YouTube...", fontSize = 11.sp, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GOLD_ACCENT, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GOLD_BRIGHT,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = CARD_DARK,
                        unfocusedContainerColor = CARD_DARK
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(220.dp).height(42.dp)
                )

                // Close Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // 2. MAIN CONTENT AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                when (selectedTab) {
                    0 -> { // TAB BÀI HÁT
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(tracks.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }) { track ->
                                TrackItemRow(
                                    track = track,
                                    isSelected = track.id == currentTrack.id,
                                    onClick = { currentTrack = track }
                                )
                            }
                        }
                    }
                    1 -> { // TAB ALBUM
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(albums) { album ->
                                AlbumCard(album = album, onClick = {})
                            }
                        }
                    }
                    2 -> { // TAB NGHỆ SĨ
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(artists) { artist ->
                                ArtistCard(artist = artist, onClick = {})
                            }
                        }
                    }
                }
            }

            // 3. BOTTOM NOW PLAYING CONTROLLER BAR
            BottomPlayerBar(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                progress = playbackProgress,
                onPlayPauseToggle = { isPlaying = !isPlaying },
                onProgressChange = { playbackProgress = it }
            )
        }
    }
}

@Composable
private fun TrackItemRow(
    track: MusicTrack,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) GOLD_ACCENT.copy(alpha = 0.16f) else CARD_DARK)
            .focusable(interactionSource = interactionSource)
            .then(
                if (isFocused) Modifier.border(2.dp, GOLD_BRIGHT, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) GOLD_ACCENT else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Equalizer else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else GOLD_ACCENT,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = track.title,
                    color = if (isSelected) GOLD_BRIGHT else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(text = track.duration, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun AlbumCard(album: MusicAlbum, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CARD_DARK)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2B2215), Color(0xFF14120E)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = GOLD_ACCENT, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(text = album.title, color = GOLD_BRIGHT, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = album.trackCount, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
private fun ArtistCard(artist: MusicArtist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CARD_DARK)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(GOLD_ACCENT.copy(alpha = 0.4f), Color(0xFF14120E)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = GOLD_BRIGHT, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(text = artist.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = artist.fans, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
private fun BottomPlayerBar(
    currentTrack: MusicTrack,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseToggle: () -> Unit,
    onProgressChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Black.copy(alpha = 0.9f))
            .border(width = 1.dp, color = GOLD_ACCENT.copy(alpha = 0.35f))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Track info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.width(220.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GOLD_ACCENT),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Headphones, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(text = currentTrack.title, color = GOLD_BRIGHT, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = currentTrack.artist, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Center Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = GOLD_ACCENT, modifier = Modifier.size(22.dp))
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GOLD_ACCENT)
                    .clickable(onClick = onPlayPauseToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = GOLD_ACCENT, modifier = Modifier.size(22.dp))
            }
        }

        // Progress Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(260.dp)
        ) {
            Text("01:45", color = Color.Gray, fontSize = 9.sp)
            Slider(
                value = progress,
                onValueChange = onProgressChange,
                colors = SliderDefaults.colors(
                    thumbColor = GOLD_BRIGHT,
                    activeTrackColor = GOLD_ACCENT,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.weight(1f).height(16.dp)
            )
            Text(currentTrack.duration, color = Color.Gray, fontSize = 9.sp)
        }
    }
}
