package com.drson.launcher.music

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MusicState(
    val title: String = "Zing MP3 Ready",
    val artist: String = "Chạm để mở nhạc",
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val volume: Float = 0.5f
)

object ZingMusicController {
    private val _musicState = MutableStateFlow(MusicState())
    val musicState: StateFlow<MusicState> = _musicState

    fun updateState(title: String, artist: String, isPlaying: Boolean, progress: Float) {
        _musicState.value = _musicState.value.copy(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            progress = progress
        )
    }

    // Gửi lệnh Media Button tới Zing MP3 (com.zing.mp3)
    private fun sendMediaKey(context: Context, keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        
        try {
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
        } catch (_: Exception) {
            // Fallback gửi Broadcast nếu AudioManager bị chặn
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                setPackage("com.zing.mp3")
                putExtra(Intent.EXTRA_KEY_EVENT, downEvent)
            }
            context.sendBroadcast(intent)
        }
    }

    fun playPause(context: Context) {
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val current = _musicState.value.isPlaying
        _musicState.value = _musicState.value.copy(isPlaying = !current)
    }

    fun next(context: Context) {
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previous(context: Context) {
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun seekTo(context: Context, positionFraction: Float) {
        _musicState.value = _musicState.value.copy(progress = positionFraction)
        // Zing MP3 xử lý tua thông qua MediaSession callback tiêu chuẩn
    }

    fun setVolume(context: Context, volumeFraction: Float) {
        _musicState.value = _musicState.value.copy(volume = volumeFraction)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVol = (maxVol * volumeFraction).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
    }
}
