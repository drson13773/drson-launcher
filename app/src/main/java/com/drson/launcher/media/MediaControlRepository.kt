package com.drson.launcher.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.drson.launcher.notifications.LauncherNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val appLabel: String,
    val albumArt: android.graphics.Bitmap?,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

/**
 * Đọc thông tin bài đang phát từ BẤT KỲ app nhạc nào (ZingMP3, Spotify, YouTube Music...) qua
 * `MediaSessionManager` - API chuẩn của Android, không cần SDK riêng của từng app. Cần cùng
 * quyền "Notification access" mà Trung tâm thông báo đã xin (MediaSessionManager bắt buộc phải
 * truyền vào 1 NotificationListenerService đã được bật làm điều kiện cấp quyền đọc media session).
 */
object MediaControlRepository {
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    private var activeController: MediaController? = null
    private var appContext: Context? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish()
        }
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            publish()
        }
        override fun onSessionDestroyed() {
            activeController = null
            _nowPlaying.value = null
        }
    }

    fun start(context: Context) {
        appContext = context.applicationContext
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(context, LauncherNotificationListenerService::class.java)

        fun refresh() {
            try {
                val sessions = manager.getActiveSessions(component)
                // Nếu có nhiều app cùng giữ media session (vd Zing MP3 đang phát + 1 app khác chỉ
                // đứng im), ưu tiên chọn đúng phiên ĐANG PHÁT thay vì luôn lấy phiên đầu danh sách -
                // tránh tình trạng thanh nhạc "kẹt" ở app không phát nhạc.
                val controller = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                    ?: sessions.firstOrNull()
                if (controller?.sessionToken != activeController?.sessionToken) {
                    activeController?.unregisterCallback(controllerCallback)
                    activeController = controller
                    controller?.registerCallback(controllerCallback)
                }
                publish()
            } catch (e: SecurityException) {
                // Chưa được cấp quyền Notification access - Trung tâm thông báo đã có màn hình xin quyền này.
                _nowPlaying.value = null
            }
        }

        try {
            manager.addOnActiveSessionsChangedListener({ refresh() }, component)
        } catch (e: SecurityException) {
            // như trên
        }
        refresh()
    }

    private fun publish() {
        val controller = activeController ?: run { _nowPlaying.value = null; return }
        val metadata = controller.metadata
        val playback = controller.playbackState
        val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val art = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
        val isPlaying = playback?.state == PlaybackState.STATE_PLAYING
        val duration = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playback?.position ?: 0L
        val appLabel = try {
            val pm = appContext?.packageManager
            val appInfo = pm?.getApplicationInfo(controller.packageName, 0)
            appInfo?.let { pm.getApplicationLabel(it).toString() } ?: controller.packageName
        } catch (e: Exception) {
            controller.packageName
        }
        if (title.isBlank() && artist.isBlank()) {
            _nowPlaying.value = null
        } else {
            _nowPlaying.value = NowPlaying(title, artist, isPlaying, appLabel, art, position, duration)
        }
    }

    /** Vị trí phát hiện tại, tính nội suy theo thời gian thực khi đang phát - để seekbar chạy mượt. */
    fun getLivePositionMs(): Long {
        val state = activeController?.playbackState ?: return 0L
        return if (state.state == PlaybackState.STATE_PLAYING) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            (state.position + elapsed * state.playbackSpeed).toLong().coerceAtLeast(0L)
        } else {
            state.position
        }
    }

    fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs)
    }

    fun playPause() {
        val controller = activeController ?: return
        val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) controller.transportControls.pause() else controller.transportControls.play()
    }

    fun next() {
        activeController?.transportControls?.skipToNext()
    }

    fun previous() {
        activeController?.transportControls?.skipToPrevious()
    }
}
