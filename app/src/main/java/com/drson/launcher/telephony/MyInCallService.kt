package com.drson.launcher.telephony

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.drson.launcher.ui.dialer.InCallActivity

/**
 * Android tự bind service này bất cứ khi nào có cuộc gọi đang diễn ra - MIỄN LÀ app này đã
 * được người dùng chọn làm "Ứng dụng Điện thoại mặc định" (Default Phone app). Áp dụng cho cả
 * cuộc gọi qua SIM của chính đầu màn hình LẪN cuộc gọi Bluetooth-relay từ điện thoại (do đầu máy
 * của bạn đã xác nhận tích hợp Bluetooth call vào đúng Telecom chuẩn - xem hội thoại trước).
 */
class MyInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallRepository.onCallAdded(call)
        instance = this
        val intent = Intent(this, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallRepository.onCallRemoved(call)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    companion object {
        var instance: MyInCallService? = null
            private set
    }
}
