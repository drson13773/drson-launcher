package com.drson.launcher.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drson.launcher.R
import com.drson.launcher.data.HomeLayoutRepository
import com.drson.launcher.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeLayoutRepository(application)
    
    var apps: List<AppItem> = emptyList()
        private set

    var dockSlots: List<String?> = listOf(null, null, null, null)
        private set

    // Trạng thái tốc độ thời gian thực (km/h)
    private val _currentSpeed = mutableFloatStateOf(0f)
    val currentSpeed: State<Float> = _currentSpeed

    private var locationManager: LocationManager? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.hasSpeed()) {
                // Đổi m/s sang km/h
                val speedKmh = location.speed * 3.6f
                _currentSpeed.floatValue = if (speedKmh < 1.5f) 0f else speedKmh
            } else {
                _currentSpeed.floatValue = 0f
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            _currentSpeed.floatValue = 0f
        }
    }

    init {
        loadInstalledApps()
        loadDockSlots()
        initGpsSpeedListener()
    }

    @SuppressLint("MissingPermission")
    fun initGpsSpeedListener() {
        try {
            locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val finePerm = ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION)
            if (finePerm == PackageManager.PERMISSION_GRANTED) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    locationListener
                )
            }
        } catch (_: Exception) {}
    }

    fun loadDockSlots() {
        dockSlots = repository.getDockSlots()
    }

    fun setDockSlot(context: Context, index: Int, packageName: String?) {
        repository.setDockSlot(index, packageName)
        loadDockSlots()
    }

    fun appFor(packageName: String): AppItem? {
        return apps.firstOrNull { it.packageName == packageName }
    }

    fun launchApp(context: Context, app: AppItem) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm: PackageManager = getApplication<Application>().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
            val appItemList = mutableListOf<AppItem>()

            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                if (pkgName == getApplication<Application>().packageName) continue

                val label = info.loadLabel(pm).toString()
                val isCameraApp = pkgName.lowercase().contains("camera") || label.lowercase().contains("camera")
                val customIcon = if (isCameraApp) {
                    ContextCompat.getDrawable(getApplication(), R.drawable.icon_camera_gold) ?: info.loadIcon(pm)
                } else {
                    info.loadIcon(pm)
                }

                appItemList.add(
                    AppItem(
                        label = label,
                        packageName = pkgName,
                        icon = customIcon
                    )
                )
            }

            appItemList.sortBy { it.label.lowercase() }

            withContext(Dispatchers.Main) {
                apps = appItemList
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (_: Exception) {}
    }
}
