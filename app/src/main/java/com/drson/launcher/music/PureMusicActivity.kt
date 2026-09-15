package com.drson.launcher.music

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drson.launcher.R
import kotlinx.coroutines.launch

class PureMusicActivity : ComponentActivity() {

    private lateinit var controller: PurePlayerController
    private lateinit var trackAdapter: MusicTrackAdapter
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvCurrentTitle: TextView
    private lateinit var tvCurrentArtist: TextView
    private lateinit var btnPlayPause: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pure_music)

        pbLoading = findViewById(R.id.pbLoading)
        tvCurrentTitle = findViewById(R.id.tvCurrentTitle)
        tvCurrentArtist = findViewById(R.id.tvCurrentArtist)
        btnPlayPause = findViewById(R.id.btnPlayPause)

        val rvTracks = findViewById<RecyclerView>(R.id.rvMusicTracks)
        rvTracks.layoutManager = GridLayoutManager(this, 2)

        trackAdapter = MusicTrackAdapter(emptyList()) { track ->
            tvCurrentTitle.text = track.title
            tvCurrentArtist.text = track.artist
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            Toast.makeText(this, "Đang tải bài: ${track.title}", Toast.LENGTH_SHORT).show()
            controller.playTrack(track, lifecycleScope)
        }
        rvTracks.adapter = trackAdapter

        controller = PurePlayerController(this)
        controller.connect()

        setupSearch()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnPlayPause.setOnClickListener {
            controller.togglePlayPause()
        }

        // Tự động tải gợi ý nhạc lúc khởi động
        performSearch("Nhạc acoustic chill")
    }

    private fun setupSearch() {
        val etSearch = findViewById<EditText>(R.id.etSearchQuery)
        val btnSearch = findViewById<Button>(R.id.btnSearch)

        val doSearch = {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            }
        }

        btnSearch.setOnClickListener { doSearch() }
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
                true
            } else false
        }
    }

    private fun performSearch(query: String) {
        pbLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val results = YouTubeAudioExtractor.searchTracks(query)
            pbLoading.visibility = View.GONE
            if (results.isNotEmpty()) {
                trackAdapter.updateData(results)
            } else {
                Toast.makeText(this@PureMusicActivity, "Không tìm thấy kết quả", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.release()
    }
}
