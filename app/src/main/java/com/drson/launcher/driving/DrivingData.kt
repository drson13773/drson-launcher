package com.drson.launcher.driving

data class DrivingData(
    val speedKmh: Float = 0f,
    val headingDegrees: Float = 0f,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Float? = null,
    val hasGpsFix: Boolean = false,
)
