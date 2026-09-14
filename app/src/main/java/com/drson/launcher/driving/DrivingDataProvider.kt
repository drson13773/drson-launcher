package com.drson.launcher.driving

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Đọc tốc độ + vị trí GPS thật qua `LocationManager`, và hướng la bàn thật qua cảm biến xoay
 * (`Sensor.TYPE_ROTATION_VECTOR`) của `SensorManager` - đều là API chuẩn Android, không cần
 * SDK riêng của hãng đầu màn hình.
 *
 * LƯU Ý: đây là tốc độ/hướng tính từ GPS + cảm biến CỦA THIẾT BỊ ANDROID, không phải đọc từ
 * đồng hồ tốc độ/CAN bus thật của xe - nên có thể trễ hoặc lệch chút so với xe (đặc biệt khi
 * mới khởi động, tín hiệu GPS yếu, hoặc đầu màn hình lắp lệch hướng xe).
 */
class DrivingDataProvider(private val context: Context) : SensorEventListener, LocationListener {

    private val _data = MutableStateFlow(DrivingData())
    val data: StateFlow<DrivingData> = _data

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    fun start() {
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            provider?.let {
                locationManager.requestLocationUpdates(it, 1000L, 3f, this)
                locationManager.getLastKnownLocation(it)?.let(::onLocationChanged)
            }
        } catch (e: SecurityException) {
            // Chưa được cấp quyền ACCESS_FINE_LOCATION - UI (DrivingRoadBackground) tự hiện lời nhắc xin quyền.
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        try {
            locationManager.removeUpdates(this)
        } catch (e: SecurityException) {
            // Không có quyền để bắt đầu thì cũng không có gì để dừng.
        }
    }

    override fun onLocationChanged(location: Location) {
        _data.value = _data.value.copy(
            speedKmh = (location.speed * 3.6f).coerceAtLeast(0f), // m/s -> km/h
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitude.toFloat(),
            hasGpsFix = true,
        )
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val degrees = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
        _data.value = _data.value.copy(headingDegrees = degrees)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Suppress("DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
