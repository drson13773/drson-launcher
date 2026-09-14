package com.drson.launcher.telephony

import android.telecom.Call

/**
 * Bọc lại 1 đối tượng android.telecom.Call thành dữ liệu đơn giản cho UI Compose quan sát,
 * tránh UI phải tự theo dõi Call.Callback ở nhiều nơi.
 */
data class CallUiState(
    val call: Call,
    val displayName: String,
    val number: String,
    val state: Int, // Call.STATE_RINGING, STATE_ACTIVE, STATE_DIALING, v.v.
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
)

data class ContactEntry(
    val id: Long,
    val name: String,
    val number: String,
)

data class CallLogEntry(
    val id: Long,
    val name: String,
    val number: String,
    val type: Int, // CallLog.Calls.INCOMING_TYPE / OUTGOING_TYPE / MISSED_TYPE
    val timestampMs: Long,
)
