package com.drson.launcher.hardware

/**
 * Trung gian cho các chức năng riêng của xe (điều hoà, sấy kính, camera lùi...).
 * Mỗi hãng đầu màn hình (Zestech, Vitech, Junsun, Hizpo...) có SDK/AIDL riêng để gọi các API
 * phần cứng xe - bạn cần viết 1 implementation khác của interface này gọi đúng SDK hãng mình
 * dùng, rồi thay [StubCarHardwareController] bằng implementation đó khi khởi tạo ViewModel.
 */
interface CarHardwareController {
    fun isAcOn(): Boolean
    fun toggleAc()
    fun isRearDefrostOn(): Boolean
    fun toggleRearDefrost()
    fun openRearCamera()
}

/**
 * Cài đặt tạm - CHƯA nối với phần cứng thật. Chỉ giữ trạng thái trong bộ nhớ để UI
 * Control Center chạy được ngay, không bị crash khi chưa có SDK hãng xe.
 */
class StubCarHardwareController : CarHardwareController {
    private var acOn = false
    private var defrostOn = false

    override fun isAcOn(): Boolean = acOn

    override fun toggleAc() {
        acOn = !acOn
        // TODO: gọi SDK/AIDL của hãng đầu màn hình để bật/tắt điều hoà thật.
    }

    override fun isRearDefrostOn(): Boolean = defrostOn

    override fun toggleRearDefrost() {
        defrostOn = !defrostOn
        // TODO: gọi SDK/AIDL của hãng đầu màn hình để bật/tắt sấy kính sau thật.
    }

    override fun openRearCamera() {
        // TODO: gọi Intent hoặc AIDL riêng của hãng đầu màn hình để mở luồng camera lùi thật.
    }
}
