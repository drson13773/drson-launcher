package com.drson.launcher.music

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PurePlayerController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    fun connect(onConnected: () -> Unit = {}) {
        val sessionToken = SessionToken(context, ComponentName(context, PureAudioService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            onConnected()
        }, MoreExecutors.directExecutor())
    }

    fun playTrack(track: PureTrack, scope: CoroutineScope) {
        scope.launch {
            val directAudioUrl = if (track.audioUrl.isNotEmpty()) {
                track.audioUrl
            } else {
                YouTubeAudioExtractor.extractStreamUrl(track.id)
            }

            if (!directAudioUrl.isNullOrEmpty()) {
                val mediaItem = MediaItem.Builder()
                    .setUri(directAudioUrl)
                    .setMediaId(track.id)
                    .build()

                mediaController?.let { player ->
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
            }
        }
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
