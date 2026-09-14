package com.drson.launcher.telephony

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * InCallService được hệ thống Android khởi tạo/gọi độc lập, không đi qua Activity nào - cần nơi
 * trung gian để đẩy trạng thái cuộc gọi hiện tại sang UI Compose (DialerActivity/InCallActivity).
 */
object CallRepository {
    private val _currentCall = MutableStateFlow<CallUiState?>(null)
    val currentCall: StateFlow<CallUiState?> = _currentCall

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            updateFrom(call)
        }
    }

    fun onCallAdded(call: Call) {
        call.registerCallback(callCallback)
        updateFrom(call)
    }

    fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)
        if (_currentCall.value?.call == call) {
            _currentCall.value = null
        }
    }

    private fun updateFrom(call: Call) {
        val details = call.details
        val number = details?.handle?.schemeSpecificPart ?: ""
        val name = details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: details?.contactDisplayName?.takeIf { it.isNotBlank() }
            ?: number.ifBlank { "Không rõ số" }
        _currentCall.value = CallUiState(
            call = call,
            displayName = name,
            number = number,
            state = call.state,
        )
    }

    fun answer() {
        _currentCall.value?.call?.answer(0)
    }

    fun reject() {
        _currentCall.value?.call?.reject(false, null)
    }

    fun hangUp() {
        _currentCall.value?.call?.disconnect()
    }

    fun toggleMute(audioManager: android.media.AudioManager) {
        audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
        val cur = _currentCall.value ?: return
        _currentCall.value = cur.copy(isMuted = audioManager.isMicrophoneMute)
    }

    fun setSpeaker(inCallService: android.telecom.InCallService, on: Boolean) {
        inCallService.setAudioRoute(
            if (on) android.telecom.CallAudioState.ROUTE_SPEAKER else android.telecom.CallAudioState.ROUTE_EARPIECE
        )
        val cur = _currentCall.value ?: return
        _currentCall.value = cur.copy(isSpeakerOn = on)
    }
}
