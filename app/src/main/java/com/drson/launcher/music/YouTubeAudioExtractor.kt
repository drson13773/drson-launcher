package com.drson.launcher.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class PureTrack(
    val id: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val durationSeconds: Long,
    val thumbnailUrl: String
)

object YouTubeAudioExtractor {

    private var isInitialized = false

    private fun ensureInit() {
        if (!isInitialized) {
            NewPipe.init(PureDownloader.instance)
            isInitialized = true
        }
    }

    suspend fun searchTracks(query: String): List<PureTrack> = withContext(Dispatchers.IO) {
        ensureInit()
        try {
            val searchExtractor = ServiceList.YouTube.getSearchExtractor(query) as SearchExtractor
            searchExtractor.fetchPage()

            searchExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    PureTrack(
                        id = item.url,
                        title = item.name,
                        artist = item.uploaderName ?: "YouTube",
                        audioUrl = "",
                        durationSeconds = item.duration,
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url ?: ""
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun extractStreamUrl(webUrl: String): String? = withContext(Dispatchers.IO) {
        ensureInit()
        try {
            val extractor = ServiceList.YouTube.getStreamExtractor(webUrl) as YoutubeStreamExtractor
            extractor.fetchPage()
            val audioStreams: List<AudioStream> = extractor.audioStreams
            val bestAudio = audioStreams.maxByOrNull { it.averageBitrate }
            bestAudio?.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
